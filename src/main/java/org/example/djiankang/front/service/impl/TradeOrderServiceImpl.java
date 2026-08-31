package org.example.djiankang.front.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.wechat.pay.java.service.partnerpayments.nativepay.model.Transaction;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.djiankang.common.PageResult;
import org.example.djiankang.config.socket.MessagePushEndpoint;
import org.example.djiankang.db.entity.GoodsSnapshotEntity;
import org.example.djiankang.db.entity.TradeOrderEntity;
import org.example.djiankang.db.mapper.GoodsSnapshotDao;
import org.example.djiankang.db.mapper.MedExamPackageMapper;
import org.example.djiankang.db.mapper.TradeOrderMapper;
import org.example.djiankang.exception.HisException;
import org.example.djiankang.front.service.PaymentService;
import org.example.djiankang.front.service.TradeOrderService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TradeOrderServiceImpl implements TradeOrderService {

    private final TradeOrderMapper tradeOrderMapper;
    private final MedExamPackageMapper medExamPackageMapper;
    private final PaymentService  paymentService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final GoodsSnapshotDao goodsSnapshotDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createPayment(Integer customerId, Integer goodsId, Integer quantity) {
        log.info("开始创建支付订单，用户：{}，商品：{}，数量：{}", customerId, goodsId, quantity);

        // 风控校验
        log.debug("检查用户[{}]是否触发风控限制", customerId);
        if (tradeOrderMapper.isCustomerReachedDailyLimit(customerId)) {
            log.warn("用户[{}]触发风控限制，拒绝创建订单", customerId);
            return null;
        }

        // 查找商品详情
        Map<String, Object> map = medExamPackageMapper.selectPackageWithPromotionForOrder(goodsId);
        if (map == null || map.isEmpty()) {
            log.warn("套餐[{}]不存在", goodsId);
            throw new HisException("套餐不存在", 404);
        }
        log.debug("查询到套餐信息：{}", map);

        // 提取基础字段
        String packageCode = MapUtil.getStr(map, "packageCode");
        String packageName = MapUtil.getStr(map, "packageName");
        String description = MapUtil.getStr(map, "description");
        String coverImage = MapUtil.getStr(map, "coverImage");
        String originalPrice = MapUtil.getStr(map, "originalPrice");
        String currentPrice = MapUtil.getStr(map, "currentPrice");
        String packageType = MapUtil.getStr(map, "packageType");
        String md5Hash = MapUtil.getStr(map, "md5Hash");
        String ruleContent = MapUtil.getStr(map, "ruleContent");
        String ruleName = MapUtil.getStr(map, "ruleName");

        // 将JSON字符串转换为对象
        // 如果不进行格式转换的话，从数据库中查询出来的json字符串直接传递给mongodb，
        // mongodb会认为它只是一个普通的字符串，不会将其视为mongodb的文档对象
        // 记得做判空处理。
        String temp = MapUtil.getStr(map, "departmentExam");
        List<Map> departmentExam = temp != null ? JSONUtil.toList(temp, Map.class) : null;

        temp = MapUtil.getStr(map, "labExam");
        List<Map> labExam = temp != null ? JSONUtil.toList(temp, Map.class) : null;

        temp = MapUtil.getStr(map, "medicalExam");
        List<Map> medicalExam = temp != null ? JSONUtil.toList(temp, Map.class) : null;

        temp = MapUtil.getStr(map, "otherExam");
        List<Map> otherExam = temp != null ? JSONUtil.toList(temp, Map.class) : null;

        temp = MapUtil.getStr(map, "examItems");
        List<Map> examItems = temp != null ? JSONUtil.toList(temp, Map.class) : null;

        temp = MapUtil.getStr(map, "tags");
        List<String> tags = temp != null ? JSONUtil.toList(temp, String.class) : null;

        log.info("套餐信息解析完成，用户：{}，套餐：{}，套餐名称：{}", customerId, goodsId, packageName);

        // 计算订单金额
        BigDecimal totalAmount = null;

        if (ruleContent == null) {
            // 无促销规则：总价 = 单价 × 数量
            totalAmount = new BigDecimal(currentPrice).multiply(BigDecimal.valueOf(quantity));
            log.debug("无促销规则，总价：{} × {} = {}", currentPrice, quantity, totalAmount);
        } else {
            // 有促销规则：执行规则引擎
            try {
                ExpressRunner runner = new ExpressRunner();
                DefaultContext<String, Object> context = new DefaultContext<>();
                context.put("number", quantity);
                context.put("price", currentPrice);

                Object result = runner.execute(ruleContent, context, null, true, false);
                totalAmount = new BigDecimal(result.toString());
                log.info("促销规则[{}]执行完成，输入：数量={}，单价={}，结果：{}", ruleName, quantity, currentPrice, totalAmount);
            } catch (Exception e) {
                log.error("促销规则执行异常，规则：{}，数量：{}，单价：{}", ruleContent, quantity, currentPrice, e);
                throw new HisException("执行规则引擎错误", e);
            }
        }
        // 创建微信支付单
        String outTradeNo = null;
        int totalAmountInFen = 0;
        String codeUrl = null;
        try {
            // 1. 生成交易流水号
            outTradeNo = IdUtil.simpleUUID().toUpperCase();
            log.debug("生成交易流水号：{}", outTradeNo);

            // 2. 金额转换：元 → 分（微信支付金额单位是分）
            totalAmountInFen = NumberUtil.round(NumberUtil.mul(totalAmount, BigDecimal.valueOf(100)), 0).intValue();
            log.debug("订单金额转换：{}元 = {}分", totalAmount, totalAmountInFen);

            // 3. 设置支付超时时间（20分钟后未支付自动关闭）
            String timeExpire = DateUtil.offsetMinute(new Date(), 20).toInstant().toString();

            // 4. 调用微信支付统一下单接口
            ObjectNode payResponse = paymentService.unifiedOrder(outTradeNo, totalAmountInFen, "购买体检套餐", timeExpire);

            // 5. 提取支付二维码链接
            codeUrl = payResponse.get("code_url").textValue();
            log.info("微信支付单创建成功，交易流水号：{}，二维码已生成", outTradeNo);
        } catch (Exception e) {
            log.error("创建微信支付单失败，交易流水号：{}，金额：{}分", outTradeNo, totalAmountInFen, e);
            throw new HisException("创建支付单失败，请稍后重试");
        }

        // 创建支付单缓存，设置缓存过期时间
        String key = "codeUrl_" + customerId + "_" + outTradeNo;
        redisTemplate.opsForValue().set(key, codeUrl, 20, TimeUnit.MINUTES); // 存储到缓存中

        // 创建商品快照（如果不存在）
        String snapshotId = null;
        try {
            snapshotId = goodsSnapshotDao.hasGoodsSnapshot(md5Hash);
            if (snapshotId == null) {
                // 创建商品快照数据
                GoodsSnapshotEntity goodsSnapshot = new GoodsSnapshotEntity();
                goodsSnapshot.setId(goodsId);
                goodsSnapshot.setPackageCode(packageCode);
                goodsSnapshot.setPackageName(packageName);
                goodsSnapshot.setDescription(description);
                goodsSnapshot.setCoverImage(coverImage);
                goodsSnapshot.setOriginalPrice(new BigDecimal(originalPrice));
                goodsSnapshot.setCurrentPrice(new BigDecimal(currentPrice));
                goodsSnapshot.setPackageType(packageType);
                goodsSnapshot.setMd5Hash(md5Hash);
                goodsSnapshot.setRuleContent(ruleContent);
                goodsSnapshot.setRuleName(ruleName);
                goodsSnapshot.setDepartmentExam(departmentExam);
                goodsSnapshot.setLabExam(labExam);
                goodsSnapshot.setMedicalExam(medicalExam);
                goodsSnapshot.setOtherExam(otherExam);
                goodsSnapshot.setExamItems(examItems);
                goodsSnapshot.setTags(tags);

                // 保存商品快照信息到MongoDB
                snapshotId = goodsSnapshotDao.insert(goodsSnapshot);
                log.info("商品快照创建成功，快照ID：{}，套餐：{}", snapshotId, packageName);
            } else {
                log.debug("商品快照已存在，快照ID：{}，套餐：{}", snapshotId, packageName);
            }
        } catch (Exception e) {
            log.error("保存商品快照失败，套餐ID：{}，套餐名称：{}", goodsId, packageName, e);
            throw new HisException("保存商品快照失败");
        }
        TradeOrderEntity tradeOrder = new TradeOrderEntity();
        tradeOrder.setOutTradeNo(outTradeNo);
        tradeOrder.setCustomerId(customerId);
        tradeOrder.setGoodsId(goodsId);
        tradeOrder.setGoodsTitle(packageName);
        tradeOrder.setGoodsPrice(new BigDecimal(currentPrice));
        tradeOrder.setGoodsImage(coverImage);
        tradeOrder.setGoodsDescription(description);
        tradeOrder.setQuantity(quantity);
        tradeOrder.setTotalAmount(totalAmount);
        tradeOrder.setSnapshotId(snapshotId);
        int rows = tradeOrderMapper.insert(tradeOrder);
        if (rows != 1) {
            log.error("订单保存失败，交易流水号：{}，用户：{}", outTradeNo, customerId);
            throw new HisException("订单保存失败");
        }
        log.info("订单保存成功，交易流水号：{}，用户：{}，金额：{}", outTradeNo, customerId, totalAmount);

        // 更新商品销量
        // 单条UPDATE语句是原子性的，MySQL通过行锁保证并发安全，不会出现数据不一致
        rows = medExamPackageMapper.updateSalesVolume(goodsId, quantity);
        if (rows != 1) {
            log.error("更新商品销量失败，商品ID：{}，购买数量：{}", goodsId, quantity);
            throw new HisException("更新商品销量失败");
        }
        log.info("更新商品销量成功，商品ID：{}，购买数量：{}", goodsId, quantity);

        // 生成付款二维码并转换为Base64字符串返回给前端
        // 返回Base64可以直接在页面显示二维码，避免前端额外请求URL导致二维码过期或增加服务器压力
        String qrCodeBase64 = null;
        try {
            QrConfig qrConfig = new QrConfig();
            qrConfig.setWidth(230);
            qrConfig.setHeight(230);
            qrConfig.setMargin(2);
            qrCodeBase64 = QrCodeUtil.generateAsBase64(codeUrl, qrConfig, "jpg");
            log.info("二维码生成成功，交易流水号：{}", outTradeNo);
        } catch (Exception e) {
            log.error("二维码生成失败，交易流水号：{}，二维码链接：{}", outTradeNo, codeUrl, e);
            throw new HisException("生成支付二维码失败，请稍后重试");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("qrCodeBase64", qrCodeBase64);
        result.put("outTradeNo", outTradeNo);
        log.info("订单创建完成，交易流水号：{}，用户：{}，金额：{}", outTradeNo, customerId, totalAmount);
        return result;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Async("taskExecutor") // 异步处理
    public void handlePayNotify(Transaction transaction) {
        String outTradeNo = transaction.getOutTradeNo();
        Transaction.TradeStateEnum tradeState = transaction.getTradeState();

        log.info("处理支付通知，订单号：{}，状态：{}", outTradeNo, tradeState);

        // 1. 查询订单是否存在
        TradeOrderEntity order = tradeOrderMapper.selectByOutTradeNo(outTradeNo);
        if (order == null) {
            log.warn("订单不存在，订单号：{}", outTradeNo);
            throw new HisException("订单不存在");
        }

        // 2. 幂等性处理：如果订单已支付，不再重复处理
        if (order.getOrderStatus() != null && order.getOrderStatus() == 3) {
            log.info("订单已支付，无需重复处理，订单号：{}", outTradeNo);
            return;
        }

        // 3. 判断支付结果
        if (Transaction.TradeStateEnum.SUCCESS.equals(tradeState)) {
            // 支付成功
            // 更新订单状态为已支付
            int rows = tradeOrderMapper.updatePayment(outTradeNo, transaction.getTransactionId(), 3);
            if (rows != 1) {
                log.error("更新订单状态失败，订单号：{}", outTradeNo);
                throw new HisException("更新订单状态失败");
            }
            // 发送消息给客户端
            Integer customerId = tradeOrderMapper.findCustomerIdByOutTradeNo(outTradeNo);
            if (customerId == null) {
                throw new HisException("客户不存在", 404);
            }
            JSONObject json = new JSONObject();
            json.set("result", true);
            MessagePushEndpoint.sendInfo(json.toString(), "customer_" + customerId);

            // 删除支付二维码缓存
            String cacheKey = "codeUrl_" + order.getCustomerId() + "_" + outTradeNo;
            redisTemplate.delete(cacheKey);

            log.info("支付通知处理成功，订单号：{}，交易号：{}，支付金额：{}分",
                    outTradeNo, transaction.getTransactionId(), transaction.getAmount().getTotal());

        } else if (Transaction.TradeStateEnum.CLOSED.equals(tradeState)) {
            // 订单已关闭
            log.info("订单已关闭，订单号：{}", outTradeNo);
            int rows = tradeOrderMapper.updatePayment(outTradeNo, transaction.getTransactionId(), 2);
            if (rows != 1) {
                log.error("关闭订单状态失败，订单号：{}", outTradeNo);
                throw new HisException("更新订单状态失败");
            }
        } else {
            // 其他状态
            log.warn("支付通知状态：{}，订单号：{}", tradeState, outTradeNo);
            // 可以根据需要记录日志或做其他处理
        }
    }
    @Override
    public boolean getPaymentResult(String outTradeNo) {
        log.info("开始查询支付结果，订单号：{}", outTradeNo);

        // 1. 查询订单是否存在
        TradeOrderEntity order = tradeOrderMapper.selectByOutTradeNo(outTradeNo);
        if (order == null) {
            log.warn("订单不存在，订单号：{}", outTradeNo);
            return false;
        }

        // 2. 如果订单已支付，直接返回 true
        if (order.getOrderStatus() != null && order.getOrderStatus() == 3) {
            log.info("订单已支付，订单号：{}", outTradeNo);
            return true;
        }

        // 3. 调用微信支付查询接口
        String transactionId = paymentService.getPaymentResult(outTradeNo);
        if (StrUtil.isBlank(transactionId)) {
            log.info("订单 {} 尚未支付", outTradeNo);
            return false;
        }

        // 4. 支付成功，更新订单状态
        log.info("订单 {} 支付成功，交易号：{}，更新订单状态", outTradeNo, transactionId);
        int rows = tradeOrderMapper.updatePayment(outTradeNo, transactionId, 3);
        if (rows != 1) {
            log.error("更新订单状态失败，订单号：{}", outTradeNo);
            return false;
        }

        // 5. 删除支付二维码缓存
        String cacheKey = "codeUrl_" + order.getCustomerId() + "_" + outTradeNo;
        redisTemplate.delete(cacheKey);
        log.info("订单 {} 支付处理完成", outTradeNo);

        return true;
    }
    @Override
    public PageResult<Map<String, Object>> queryPage(Map<String, Object> param) {
        long total = tradeOrderMapper.selectPageCount(param);
        List<Map<String, Object>> records = new ArrayList<>();
        if (total > 0) {
            records = tradeOrderMapper.selectPageList(param);
        }
        return PageResult.of(records, total);
    }
    @Override
    public void refund(Integer customerId, Integer orderId) {
        // 1. 查询是否已发起过退款
        String existingRefundNo = tradeOrderMapper.findOutRefundNoByOrderId(orderId);
        if (existingRefundNo != null) {
            log.warn("退款已发起，订单号：{}，退款单号：{}，请勿重复操作", orderId, existingRefundNo);
            throw new HisException("退款已发起，请勿重复操作");
        }

        // 2. 查询交易信息
        Map<String, Object> map = tradeOrderMapper.findTransactionInfoByOrderIdAndCustomerId(orderId, customerId);
        if (map == null || map.isEmpty()) {
            log.error("未查询到交易信息，订单号：{}，客户ID：{}", orderId, customerId);
            throw new HisException("未查询到交易信息");
        }

        String transactionId = MapUtil.getStr(map, "transactionId");
        String totalAmount = MapUtil.getStr(map, "totalAmount");

        if (StrUtil.isBlank(transactionId)) {
            log.error("微信支付订单号为空，订单ID：{}，客户ID：{}", orderId, customerId);
            throw new HisException("微信支付订单号为空");
        }

        // 3. 金额转换（分）
        //int total = NumberUtil.mul(totalAmount, "100").intValue();
        int total = 1;
        int refund = total;

        log.info("发起退款，订单号：{}，客户ID：{}，交易号：{}，退款金额：{}分", orderId, customerId, transactionId, refund);

        // 4. 调用微信退款
        String outRefundNo = paymentService.refund(transactionId, refund, total);
        if (StrUtil.isBlank(outRefundNo)) {
            log.error("微信退款失败，订单号：{}，交易号：{}", orderId, transactionId);
            throw new HisException("微信退款失败");
        }

        // 5. 更新退款单号
        int rows = tradeOrderMapper.updateOutRefundNoByOrderIdAndOutRefundNo(orderId, outRefundNo);
        if (rows != 1) {
            log.error("更新退款单号失败，订单号：{}，退款单号：{}", orderId, outRefundNo);
            throw new HisException("更新退款单号失败");
        }

        log.info("退款成功，订单号：{}，退款单号：{}", orderId, outRefundNo);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRefundNotify(RefundNotification refundNotification) {
        String outRefundNo = refundNotification.getOutRefundNo();
        String refundStatus = refundNotification.getRefundStatus().toString();
        String outTradeNo = refundNotification.getOutTradeNo();

        log.info("处理退款回调，退款单号：{}，状态：{}，订单号：{}", outRefundNo, refundStatus, outTradeNo);

        if ("SUCCESS".equals(refundStatus)) {
            int rows = tradeOrderMapper.updateStatusByOutTradeNo(outTradeNo);
            if (rows == 0) {
                log.warn("退款回调更新订单状态失败，订单号：{}", outTradeNo);
                throw new HisException("更新订单退款状态失败");
            }
            log.info("退款成功，退款单号：{}，订单号：{}", outRefundNo, outTradeNo);

        } else if ("ABNORMAL".equals(refundStatus)) {
            log.error("退款异常：用户银行卡冻结或作废，退款单号：{}，订单号：{}", outRefundNo, outTradeNo);
            // TODO: 发送告警通知给客服

        } else {
            log.info("退款状态：{}，退款单号：{}，暂不处理", refundStatus, outRefundNo);
        }
    }
}
