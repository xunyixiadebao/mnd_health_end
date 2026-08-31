package org.example.djiankang.front.service.impl;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.djiankang.exception.HisException;
import org.example.djiankang.front.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    @Value("${wechat.pay.v3.mnd-health.mch-id}")
    private String mchId;

    @Value("${wechat.pay.v3.mnd-health.app-id}")
    private String appId;

    @Value("${wechat.pay.v3.mnd-health.notify-url}")
    private String notifyUrl;
    @Value("${wechat.pay.v3.mnd-health.refund-notify-url}")
    private String refundNotifyUrl;

    private final Config wechatPayConfig;

    private final ObjectMapper objectMapper;

    @Override
    public ObjectNode unifiedOrder(String outTradeNo, int total, String desc, String timeExpire) {
        try {
            NativePayService nativePayService = new NativePayService.Builder().config(wechatPayConfig).build();

            PrepayRequest request = new PrepayRequest();
            request.setAppid(appId);
            request.setMchid(mchId);
            request.setDescription(desc);
            request.setOutTradeNo(outTradeNo);
            request.setNotifyUrl(notifyUrl);

            Amount amount = new Amount();
            // amount.setTotal(total); // 生产环境用真实金额
            amount.setTotal(1);        // 测试环境用1分钱
            amount.setCurrency("CNY");
            request.setAmount(amount);

            if (timeExpire != null && !timeExpire.isEmpty()) {
                request.setTimeExpire(timeExpire);
            }

            log.info("创建Native支付订单，订单号：{}，金额：{}分", outTradeNo, amount.getTotal());
            PrepayResponse response = nativePayService.prepay(request);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("code_url", response.getCodeUrl());
            result.put("out_trade_no", outTradeNo);

            log.info("Native支付订单创建成功，订单号：{}", outTradeNo);

            return result;

        } catch (Exception e) {
            log.error("创建Native支付订单失败，订单号：{}", outTradeNo, e);
            throw new HisException("创建微信支付订单失败：" + e.getMessage(), e);
        }
    }
    @Override
    public String getPaymentResult(String outTradeNo) {
        try {
            NativePayService nativePayService = new NativePayService.Builder().config(wechatPayConfig).build();

            QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
            request.setMchid(mchId);
            request.setOutTradeNo(outTradeNo);

            Transaction transaction = nativePayService.queryOrderByOutTradeNo(request);

            String tradeState = transaction.getTradeState() != null ? transaction.getTradeState().toString() : "UNKNOWN";

            log.info("查询支付结果，订单号：{}，状态：{}", outTradeNo, tradeState);

            // 支付成功返回 transaction_id，否则返回 null
            if ("SUCCESS".equals(tradeState)) {
                String transactionId = transaction.getTransactionId();
                log.info("订单 {} 支付成功，微信支付订单号：{}", outTradeNo, transactionId);
                return transactionId;
            } else {
                log.info("订单 {} 支付状态：{}", outTradeNo, tradeState);
                return null;
            }

        } catch (Exception e) {
            log.error("查询支付结果失败，订单号：{}", outTradeNo, e);
            return null;
        }
    }
    @Override
    public String refund(String transactionId, Integer refund, Integer total) {
        // 1. 生成退款流水号
        String outRefundNo = IdUtil.simpleUUID().toUpperCase();

        // 2. 构建退款请求
        CreateRequest request = new CreateRequest();
        request.setTransactionId(transactionId);
        request.setOutRefundNo(outRefundNo);
        request.setNotifyUrl(refundNotifyUrl);

        AmountReq amount = new AmountReq();
        amount.setRefund(Long.valueOf(refund));
        amount.setTotal(Long.valueOf(total));
        amount.setCurrency("CNY");
        request.setAmount(amount);

        log.info("发起微信退款，原订单号：{}，退款单号：{}，退款金额：{}分，原金额：{}分",
                transactionId, outRefundNo, refund, total);

        // 3. 创建退款服务并调用
        RefundService refundService = new RefundService.Builder()
                .config(wechatPayConfig).build();

        Refund response = refundService.create(request);

        // 4. 处理响应
        String status = response.getStatus() != null ? response.getStatus().toString() : "UNKNOWN";

        log.info("微信退款响应，退款单号：{}，状态：{}，微信退款单号：{}",
                outRefundNo, status, response.getRefundId());

        if ("PROCESSING".equals(status) || "SUCCESS".equals(status)) {
            return outRefundNo;
        }

        log.warn("退款状态异常，退款单号：{}，状态：{}", outRefundNo, status);
        return null;
    }
}