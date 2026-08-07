package com.ink.core.handler;

import com.ink.channel.cmpp.CmppMessage;

/**
 * CMPP 消息处理器接口
 * 用于处理上行消息（如 Deliver）
 */
public interface CmppMessageHandler {

    /**
     * 处理收到的 CMPP 消息
     * @param message     消息对象
     * @param channelCode 消息来源的上游通道编码
     */
    void handleMessage(CmppMessage message, String channelCode);

    /**
     * 连接建立并认证成功
     */
    default void onConnected() {}

    /**
     * 连接断开
     */
    default void onDisconnected() {}
}
