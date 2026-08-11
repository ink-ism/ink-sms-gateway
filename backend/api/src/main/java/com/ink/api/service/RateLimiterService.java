package com.ink.api.service;

import com.ink.api.config.RateLimitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 限流服务
 * 基于 Redis Lua 脚本的 1 秒滑动窗口限流
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private static final String SP_KEY_PREFIX = "ink:ratelimit:sp:";
    private static final String IP_KEY_PREFIX = "ink:ratelimit:ip:";

    /** 窗口大小（毫秒） */
    private static final long WINDOW_MS = 1000L;

    /**
     * 滑动窗口 Lua 脚本：清理过期成员 -> 统计窗口内数量 -> 未超限则加入新成员
     * KEYS[1]=限流key, ARGV[1]=当前时间戳(ms), ARGV[2]=窗口起点, ARGV[3]=唯一成员, ARGV[4]=上限
     * 返回：1-放行，0-限流
     */
    private static final DefaultRedisScript<Long> SLIDING_WINDOW_SCRIPT;

    static {
        SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>();
        SLIDING_WINDOW_SCRIPT.setScriptText(
                "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[2]) " +
                "local count = redis.call('ZCARD', KEYS[1]) " +
                "if count < tonumber(ARGV[4]) then " +
                "  redis.call('ZADD', KEYS[1], ARGV[1], ARGV[3]) " +
                "  redis.call('PEXPIRE', KEYS[1], ARGV[5]) " +
                "  return 1 " +
                "else " +
                "  return 0 " +
                "end");
        SLIDING_WINDOW_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final SpAccountService spAccountService;
    private final RateLimitConfig rateLimitConfig;

    /**
     * SP 维度限流判定
     * @return true-放行，false-已限速
     */
    public boolean tryAcquireSp(String spId) {
        if (spId == null) {
            return true;
        }
        SpAccountService.SpAccount account = spAccountService.findAccount(spId);
        int limit = account != null ? account.getRateLimit() : 0;
        if (limit <= 0) {
            return true; // 0 表示不限制
        }
        return doAcquire(SP_KEY_PREFIX + spId, limit);
    }

    /**
     * IP 维度限流判定（REST 入口）
     * @return true-放行，false-已限速
     */
    public boolean tryAcquireIp(String ip) {
        int limit = rateLimitConfig.getIpPerSecond();
        if (limit <= 0 || ip == null) {
            return true;
        }
        return doAcquire(IP_KEY_PREFIX + ip, limit);
    }

    /**
     * 执行滑动窗口判定
     */
    private boolean doAcquire(String key, int limit) {
        try {
            long now = System.currentTimeMillis();
            String member = now + ":" + Math.random();
            Long result = stringRedisTemplate.execute(SLIDING_WINDOW_SCRIPT,
                    Collections.singletonList(key),
                    String.valueOf(now),
                    String.valueOf(now - WINDOW_MS),
                    member,
                    String.valueOf(limit),
                    String.valueOf(WINDOW_MS + 500));
            return result != null && result == 1L;
        } catch (Exception e) {
            // Redis 故障时降级放行，避免影响发送主链路
            log.warn("限流判定异常，降级放行: key={}, error={}", key, e.getMessage());
            return true;
        }
    }
}
