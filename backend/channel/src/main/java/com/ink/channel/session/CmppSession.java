package com.ink.channel.session;

import com.ink.channel.cmpp.CmppMessage;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CMPP 会话封装
 * 管理一个 CMPP 连接的状态和 Netty Channel
 */
@Slf4j
@Data
public class CmppSession {

    /** 关联的 Netty Channel */
    private Channel channel;

    /** SP 标识 */
    private String spId;

    /** 是否已认证 */
    private final AtomicBoolean authenticated = new AtomicBoolean(false);

    /** 会话创建时间 */
    private final long createTime = System.currentTimeMillis();

    /** 最后活跃时间 */
    private volatile long lastActiveTime = System.currentTimeMillis();

    public CmppSession(Channel channel, String spId) {
        this.channel = channel;
        this.spId = spId;
    }

    /**
     * 发送消息
     */
    public void send(CmppMessage message) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message);
            lastActiveTime = System.currentTimeMillis();
        } else {
            log.warn("Channel 不可用，无法发送消息: spId={}", spId);
        }
    }

    /**
     * 标记为已认证
     */
    public void markAuthenticated() {
        authenticated.set(true);
        lastActiveTime = System.currentTimeMillis();
    }

    /**
     * 是否已认证
     */
    public boolean isAuthenticated() {
        return authenticated.get();
    }

    /**
     * 更新活跃时间
     */
    public void updateActiveTime() {
        lastActiveTime = System.currentTimeMillis();
    }

    /**
     * 关闭会话
     */
    public void close() {
        if (channel != null && channel.isActive()) {
            channel.close();
        }
        authenticated.set(false);
    }

    /**
     * 是否活跃
     */
    public boolean isActive() {
        return channel != null && channel.isActive() && authenticated.get();
    }
}
