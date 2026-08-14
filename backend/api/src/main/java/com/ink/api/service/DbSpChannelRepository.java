package com.ink.api.service;

import com.ink.core.repository.SpChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户-通道绑定查询实现（ink_sp_channel 表）
 * 性能优化：本地缓存 + 定时刷新，消除逐条 MySQL 查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbSpChannelRepository implements SpChannelRepository {

    private final JdbcTemplate jdbcTemplate;

    /** 本地缓存：spId -> channelCodes（TTL 控制刷新） */
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60_000L; // 1 分钟

    @Override
    public List<String> findChannelCodes(String spId) {
        if (spId == null || spId.isBlank()) {
            return null;
        }
        CacheEntry entry = cache.get(spId);
        if (entry != null && !entry.isExpired()) {
            return entry.codes;
        }
        // 缓存 miss 或过期，查 DB 并刷新缓存
        try {
            List<String> codes = jdbcTemplate.queryForList(
                    "SELECT channel_code FROM ink_sp_channel WHERE sp_id = ?",
                    String.class, spId);
            cache.put(spId, new CacheEntry(codes));
            return codes;
        } catch (Exception e) {
            log.error("查询客户绑定通道失败: spId={}, error={}", spId, e.getMessage());
            // 有旧缓存就用旧缓存
            return entry != null ? entry.codes : null;
        }
    }

    /** 缓存条目（带过期时间） */
    private static class CacheEntry {
        final List<String> codes;
        final long expireAt;
        CacheEntry(List<String> codes) {
            this.codes = codes;
            this.expireAt = System.currentTimeMillis() + CACHE_TTL_MS;
        }
        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
