package com.ruoyi.mall.service;

/**
 * 聊天实时推送接口（实现在 ruoyi-admin 的 WebSocket 层）。
 * 在 ruoyi-mall 业务逻辑中通过此接口解耦 Web/WS 依赖，镜像 {@link ISsePushService}。
 */
public interface IChatPushService
{
    /**
     * 向指定会员的所有在线 WS 连接发送一帧（frame 会被序列化为 JSON 文本）。
     * @param memberId 目标会员
     * @param frame    JSON-serializable 帧对象（含 type 字段）
     */
    void pushToMember(Long memberId, Object frame);

    /** 该会员当前是否有在线 WS 连接。 */
    boolean isOnline(Long memberId);
}
