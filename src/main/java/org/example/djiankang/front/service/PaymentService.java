package org.example.djiankang.front.service;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface PaymentService {

    /**
     * 微信支付统一下单接口 - Native支付（PC扫码支付）
     *
     * @param outTradeNo 商户订单号（唯一）
     * @param total      订单总金额，单位：分
     * @param desc       商品描述
     * @param timeExpire 订单过期时间（ISO8601格式，可为null）
     * @return 包含 code_url（二维码链接）等信息的JSON对象
     */
    ObjectNode unifiedOrder(String outTradeNo, int total, String desc, String timeExpire);
    /**
     * 获取支付结果
     *
     * @param outTradeNo 订单流水号
     * @return 支付结果
     */
    String getPaymentResult(String outTradeNo);
    /**
     * 退款接口
     *
     * @param transactionId 微信支付单id
     * @param refund        退款金额
     * @param total         订单总金额
     * @return 退款状态是PROCESSING时，返回退款交易流水单号，否则返回null
     */
    String refund(String transactionId, Integer refund, Integer total);
}