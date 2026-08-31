package org.example.djiankang.db.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.djiankang.db.entity.TradeOrderEntity;

import java.util.List;
import java.util.Map;

/**
* @author zl
* @description 针对表【trade_order(交易订单表)】的数据库操作Mapper
* @createDate 2026-08-17 20:34:37
* @Entity org.example.djiankang.db.entity.TradeOrderEntity
*/
public interface TradeOrderMapper {

    Map<String, Object> selectOrderStatistics(Integer id);

    // 检查客户是否达到了每日限制
    boolean isCustomerReachedDailyLimit(Integer customerId);


    //定时任务，关单
    int closeOrder();

    //保存支付订单
    int insert(TradeOrderEntity order);

    /**
     * 关闭超时未支付的订单
     * @param outTradeNo 订单流水号
     * @return 1表示关单成功
     */
    int closeOrderByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    /**
     * 更新订单表中的微信支付单编号以及订单状态
     *
     * @param outTradeNo    订单流水号
     * @param transactionId 微信支付单编号
     * @param status        订单状态
     * @return 1表示更新成功
     */
    int updatePayment(@Param("outTradeNo") String outTradeNo,
                      @Param("transactionId") String transactionId,
                      @Param("status") Integer status);

    /**
     * 根据订单流水号查询订单
     *
     * @param outTradeNo 订单流水号
     * @return 订单信息
     */
    TradeOrderEntity selectByOutTradeNo(String outTradeNo);
    /**
     * 根据订单流水号获取客户id
     *
     * @param outTradeNo 订单流水号
     * @return 客户id
     */
    Integer findCustomerIdByOutTradeNo(String outTradeNo);


    List<Map<String, Object>> selectPageList(Map<String, Object> params);

    long selectPageCount(Map<String, Object> params);
    /**
     * 根据订单id查询退款交易流水号
     *
     * @param orderId 订单id
     * @return 退款交易流水号
     */
    String findOutRefundNoByOrderId(@Param("orderId") Integer orderId);

    /**
     * 根据订单id和客户id查询 微信支付id和支付金额
     *
     * @param orderId    订单id
     * @param customerId 客户id
     * @return 微信支付id和支付金额
     */
    Map<String, Object> findTransactionInfoByOrderIdAndCustomerId(@Param("orderId") Integer orderId,
                                                                  @Param("customerId") Integer customerId);

    /**
     * 更新退款交易流水号
     *
     * @param orderId     订单id
     * @param outRefundNo 退款交易流水号
     * @return 1表示更新成功
     */
    int updateOutRefundNoByOrderIdAndOutRefundNo(@Param("orderId") Integer orderId,
                                                 @Param("outRefundNo") String outRefundNo);
    /**
     * 将订单状态更新为已退款
     *
     * @param outTradeNo 订单流水号
     * @return 1表示更新成功
     */
    int updateStatusByOutTradeNo(String outTradeNo);
}




