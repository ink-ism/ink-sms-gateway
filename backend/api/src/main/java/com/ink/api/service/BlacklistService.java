package com.ink.api.service;

import com.ink.api.config.CmppUnsubscribeConfig;
import com.ink.common.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 通道级退订黑名单服务
 * 上行命中退订关键字时登记黑名单（按通道隔离），支持过期刷新
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlacklistService {

    private final JdbcTemplate jdbcTemplate;
    private final RedisUtil redisUtil;
    private final CmppUnsubscribeConfig unsubscribeConfig;

    /**
     * 登记退订黑名单（已存在则刷新过期时间与触发信息）
     * @param channelCode 触发退订的上游通道
     * @param phone       退订手机号
     * @param keyword     命中的退订关键字
     * @param sourceMoId  触发退订的上行消息 ID
     */
    public void addByUnsubscribe(String channelCode, String phone, String keyword, String sourceMoId) {
        if (channelCode == null || phone == null || phone.isBlank()) {
            return;
        }
        int expireDays = Math.max(1, unsubscribeConfig.getBlacklistExpireDays());
        LocalDateTime expireTime = LocalDateTime.now().plusDays(expireDays);
        try {
            jdbcTemplate.update(
                    "INSERT INTO ink_blacklist (channel_code, phone, keyword, source_mo_id, expire_time) VALUES (?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE keyword = VALUES(keyword), source_mo_id = VALUES(source_mo_id), expire_time = VALUES(expire_time)",
                    channelCode, phone, keyword, sourceMoId, expireTime);
            // 同步写缓存，TTL 为过期天数
            redisUtil.set(RedisBlacklistChecker.KEY_PREFIX + channelCode + ":" + phone, "1", expireDays, TimeUnit.DAYS);
            log.info("已登记退订黑名单: channel={}, phone={}, keyword={}, 过期时间={}",
                    channelCode, phone, keyword, expireTime);
        } catch (Exception e) {
            log.error("登记退订黑名单失败: channel={}, phone={}, error={}", channelCode, phone, e.getMessage(), e);
        }
    }
}
