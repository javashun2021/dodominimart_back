package com.ruoyi.mall.service;

import java.math.BigDecimal;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * 拉卡拉收银台（线上 H5）支付服务。
 * 所有拉卡拉 SDK 调用（加签/验签/HTTP）都封装在本模块内，
 * 上层控制器只拿到普通 String / Map，不直接依赖 SDK 类。
 */
public interface ILakalaPayService
{
    /** 是否启用拉卡拉线上支付 */
    boolean isEnabled();

    /**
     * 创建收银台订单，返回收银台 H5 支付链接（counter_url）。
     *
     * @param orderNo     商城订单号（作为拉卡拉 out_order_no，用于幂等/对账）
     * @param payableYuan 应付金额（元）
     * @param subject     订单标题（order_info）
     * @param outUserId   下单会员标识（out_user_id，可空）
     * @return 收银台支付链接
     */
    String createCounterPayment(String orderNo, BigDecimal payableYuan, String subject, String outUserId);

    /**
     * 处理拉卡拉异步通知：验签并解析报文。
     *
     * @return 验签成功返回解析后的字段
     *         （orderNo=商城订单号, tradeStatus, paidAmountYuan, tradeNo）；
     *         验签失败返回 null。
     */
    Map<String, String> handleNotify(HttpServletRequest request);

    /** 通知处理成功后回给拉卡拉的标准响应体 {"code":"SUCCESS",...}，用于停止重复通知 */
    String successResponseBody();
}
