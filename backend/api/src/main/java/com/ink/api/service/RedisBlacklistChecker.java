package com.ink.api.service;

import com.ink.common.utils.RedisUtil;
import com.ink.core.repository.BlacklistChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 通道级黑名单判定实现
 * Redis 缓存优先，miss 时查数据库兆底（过期记录惰性失效并回填缓存）
 * 性能优化：本地负缓存（channel:phone -> not-blocked），消除逐条 Redis+MySQL 查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisBlacklistChecker implements BlacklistChecker {

    /** Redis key 前缀：ink:blacklist:{channelCode}:{phone} */
    public static final String KEY_PREFIX = "ink:blacklist:";

    private final RedisUtil redisUtil;
    private final JdbcTemplate jdbcTemplate;

    /** 本地负缓存：key -> 过期时间戳（不在黑名单中的号码快速放行） */
    private final ConcurrentHashMap<String, Long> negativeCache = new ConcurrentHashMap<>();
    private static final long NEGATIVE_CACHE_TTL_MS = 30_000L; // 30 秒

    @Override
    public boolean isBlocked(String channelCode, String phone) {
        if (channelCode == null || phone == null || phone.isBlank()) {
            return false;
        }
        String key = KEY_PREFIX + channelCode + ":" + phone;

        // 本地负缓存快速放行
        Long expireAt = negativeCache.get(key);
        if (expireAt != null && System.currentTimeMillis() < expireAt) {
            return false; // 已知不在黑名单
        }

        try {
            if (redisUtil.hasKey(key)) {
                negativeCache.remove(key);
                return true;
            }
            // DB 兆底：仅未过期记录生效
            Long remainSec = jdbcTemplate.queryForObject(
                    "SELECT TIMESTAMPDIFF(SECOND, NOW(), expire_time) FROM ink_blacklist " +
                    "WHERE channel_code = ? AND phone = ? AND expire_time > NOW() LIMIT 1",
                    Long.class, channelCode, phone);
            if (remainSec != null && remainSec > 0) {
                // 回填缓存，TTL 为剩余有效期
                redisUtil.set(key, "1", remainSec, TimeUnit.SECONDS);
                negativeCache.remove(key);
                return true;
            }
            // 放入负缓存，30s 内不再查 Redis/DB
            negativeCache.put(key, System.currentTimeMillis() + NEGATIVE_CACHE_TTL_MS);
            return false;
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // 无记录，放入负缓存
            negativeCache.put(key, System.currentTimeMillis() + NEGATIVE_CACHE_TTL_MS);
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
