package com.ruoyi.mall.service;

import java.util.Map;

/**
 * 对外「订单导入」编排服务。
 * 接收外部已支付/未支付交易 → find-or-create 会员 → 按付款价匹配商品(单品/组合) →
 * 差价用积分+补差券补齐 → 平台补助浮动实付 → 建真实商城订单(已支付则模拟配送闭环)。
 */
public interface IExternalOrderService
{
    /**
     * 导入一笔外部订单。
     *
     * @param outOrderNo    外部订单号(必填,幂等去重)
     * @param amountStr     付款价(元,两位小数,必填)
     * @param userId        外部会员id(必填,find-or-create 会员)
     * @param phone         会员手机号(可选)
     * @param floatAmountStr 平台补助(元,[0,1),默认0.00,须≤付款价;最终实付=付款价−补助)
     * @param orderTimeStr  订单创建时间 yyyy-MM-dd HH:mm:ss(可选,缺省当前)
     * @param payTimeStr    订单支付时间 yyyy-MM-dd HH:mm:ss(可选;传了=已支付,不传=未支付)
     * @return 亿林风格 {code, msg, data{orderNo, paidAmount}}
     */
    Map<String, Object> importOrder(String outOrderNo, String amountStr, String userId, String phone,
                                    String floatAmountStr, String orderTimeStr, String payTimeStr);
}
