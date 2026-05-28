package com.ruoyi.mall.service;

/**
 * SSE 推送服务接口（实现在 ruoyi-admin 层）
 * 在 ruoyi-mall 业务逻辑中通过此接口解耦 Web 依赖。
 */
public interface ISsePushService
{
    /**
     * 向指定会员推送 SSE 事件
     * @param memberId 会员ID
     * @param event    事件名（如 "order_status"）
     * @param data     JSON-serializable 对象
     */
    void push(Long memberId, String event, Object data);
}
