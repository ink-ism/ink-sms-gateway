package com.ink.core.repository;

/**
 * 通道级黑名单判定接口
 * core 定义、api 侧实现（Redis 缓存 + 数据库兜底）
 */
public interface BlacklistChecker {

    /**
     * 判断手机号是否在指定通道的退订黑名单中（已过期的不生效）
     */
    boolean isBlocked(String channelCode, String phone);
}
