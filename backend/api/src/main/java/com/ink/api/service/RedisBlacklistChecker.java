package com.ink.api.service;

import com.ink.common.utils.RedisUtil;
import com.ink.core.repository.BlacklistChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 通道级黑名单判定实现
 * Redis 缓存优先，miss 时查数据库兜底（过期记录惰性失效并回填缓存）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisBlacklistChecker implements BlacklistChecker {

    /** Redis key 前缀：ink:blacklist:{channelCode}:{phone} */
    public static final String KEY_PREFIX = "ink:blacklist:";

    private final RedisUtil redisUtil;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean isBlocked(String channelCode, String phone) {
        if (channelCode == null || phone == null || phone.isBlank()) {
            return false;
        }
        String key = KEY_PREFIX + channelCode + ":" + phone;
        try {
            if (redisUtil.hasKey(key)) {
                return true;
            }
            // DB 兜底：仅未过期记录生效
            Long remainSec = jdbcTemplate.queryForObject(
                    "SELECT TIMESTAMPDIFF(SECOND, NOW(), expire_time) FROM ink_blacklist " +
                    "WHERE channel_code = ? AND phone = ? AND expire_time > NOW() LIMIT 1",
                    Long.class, channelCode, phone);
            if (remainSec != null && remainSec > 0) {
                // 回填缓存，TTL 为剩余有效期
                redisUtil.set(key, "1", remainSec, TimeUnit.SECONDS);
                return true;
            }
            return false;
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return false;
        } catch (Exception e) {
            // Redis/DB 异常时放行，避免影响发送主链路
            log.warn("黑名单判定异常，默认放行: channel={}, phone={}, error={}", channelCode, phone, e.getMessage());
            return false;
        }
    }

    /**
     * 移除缓存（黑名单被管理端移除或过期刷新时使用）
     */
    public void evict(String channelCode, String phone) {
        try {
            redisUtil.delete(KEY_PREFIX + channelCode + ":" + phone);
        } catch (Exception e) {
            log.warn("移除黑名单缓存失败: {}", e.getMessage());
        }
    }
}
