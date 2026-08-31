package org.example.djiankang.front.service;

import com.wechat.pay.java.service.partnerpayments.nativepay.model.Transaction;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import org.example.djiankang.common.PageResult;

import java.util.Map;

public interface TradeOrderService {
    /**
     * 创建预支付订单
     *
     * @param customerId 客户id
     * @param goodsId    商品id
     * @param quantity   购买数量
     * @return 预支付订单
     */
    Map<String, Object> createPayment(Integer customerId, Integer goodsId, Integer quantity);
    /**
     * 处理支付通知：更新订单状态
     *
     * @param transaction 微信平台返回的交易信息
     */
    void handlePayNotify(Transaction transaction);
    /**
     * 获取付款结果
     *
     * @param outTradeNo 订单流水号
     * @return true表示已付款
     */
    boolean getPaymentResult(String outTradeNo);


    PageResult<Map<String, Object>> queryPage(Map<String, Object> param);


    /**
     * 根据订单id给客户执行退款操作
     *
     * @param customerId 客户id
     * @param orderId    订单id
     */
    void refund(Integer customerId, Integer orderId);
    /**
     * 将订单状态更新为已退款
     *
     * @param refundNotification 退款通知对象
     */
    void handleRefundNotify(RefundNotification refundNotification);
}
