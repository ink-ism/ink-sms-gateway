package com.ink.api.session;

import com.ink.channel.session.CmppSession;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SP 会话管理器
 * 管理所有已认证的下游 SP 连接
 */
@Component
public class SpSessionManager {

    /** SP 会话映射：spId -> CmppSession */
    private final Map<String, CmppSession> sessions = new ConcurrentHashMap<>();

    /**
     * 注册会话
     */
    public void addSession(String spId, CmppSession session) {
        // 关闭旧会话
        CmppSession old = sessions.put(spId, session);
        if (old != null) {
            old.close();
        }
    }

    /**
     * 获取会话
     */
    public CmppSession getSession(String spId) {
        return sessions.get(spId);
    }

    /**
     * 移除会话（带实例比对）
     * 仅当当前注册的会话与传入实例相同时才移除，
     * 避免同 spId 重连时旧连接断开回调误删新会话
     */
    public void removeSession(String spId, CmppSession session) {
        if (session == null) {
            removeSession(spId);
            return;
        }
        if (sessions.remove(spId, session)) {
            session.close();
        }
    }

    /**
     * 移除会话（按 spId，不区分实例）
     */
    public void removeSession(String spId) {
        CmppSession session = sessions.remove(spId);
        if (session != null) {
            session.close();
        }
    }

    /**
     * 获取所有活跃会话数
     */
    public int getActiveCount() {
        return (int) sessions.values().stream().filter(CmppSession::isActive).count();
    }

    /**
     * 验证 SP 认证信息
     */
    public boolean isValidSp(String spId, String secret, String allowedSpList) {
        if (allowedSpList == null || allowedSpList.isEmpty()) {
            return false;
        }
        for (String entry : allowedSpList.split(",")) {
            String[] parts = entry.trim().split(":");
            if (parts.length == 2 && parts[0].equals(spId) && parts[1].equals(secret)) {
                return true;
            }
        }
        return false;
    }
}
