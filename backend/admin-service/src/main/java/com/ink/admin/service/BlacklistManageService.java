package com.ink.admin.service;

import com.ink.admin.entity.Blacklist;
import com.ink.admin.mapper.BlacklistMapper;
import com.ink.common.exception.BusinessException;
import com.ink.common.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 退订黑名单管理服务类
 * 提供分页查询、手动移除与手动加黑能力，移除/加黑时同步清理 api 侧黑名单缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlacklistManageService {

    /** 与 api 侧 RedisBlacklistChecker 保持一致的缓存 key 前缀 */
    private static final String CACHE_KEY_PREFIX = "ink:blacklist:";

    /** 手动加黑的默认过期天数 */
    private static final int DEFAULT_BLACKLIST_DAYS = 180;

    private final BlacklistMapper blacklistMapper;
    private final RedisUtil redisUtil;

    /**
     * 分页查询黑名单（支持通道/手机号过滤）
     */
    public List<Blacklist> getBlacklist(String channelCode, String phone, int page, int size) {
        return blacklistMapper.findByPage(channelCode, phone, (page - 1) * size, size);
    }

    /**
     * 查询黑名单总数
     */
    public int getBlacklistCount(String channelCode, String phone) {
        return blacklistMapper.count(channelCode, phone);
    }

    /**
     * 移除黑名单（同步清理 Redis 缓存）
     */
    @Transactional
    public void removeBlacklist(Long id) {
        Blacklist blacklist = blacklistMapper.findById(id);
        if (blacklist == null) {
            throw new BusinessException("黑名单记录不存在");
        }
        blacklistMapper.deleteById(id);
        try {
            redisUtil.delete(CACHE_KEY_PREFIX + blacklist.getChannelCode() + ":" + blacklist.getPhone());
        } catch (Exception e) {
            log.warn("移除黑名单缓存失败: {}", e.getMessage());
        }
        log.info("黑名单移除成功: id={}, channel={}, phone={}", id, blacklist.getChannelCode(), blacklist.getPhone());
    }

    /**
     * 手动将手机号加入黑名单（同步写 Redis 缓存）
     *
     * @param channelCode 通道编码
     * @param phone       手机号
     * @param sourceMoId  关联的上行消息 ID（可为 null）
     */
    @Transactional
    public void addBlacklist(String channelCode, String phone, String sourceMoId) {
        if (channelCode == null || channelCode.isBlank() || phone == null || phone.isBlank()) {
            throw new BusinessException("通道编码和手机号不能为空");
        }
        // 检查是否已存在有效黑名单
        int exists = blacklistMapper.existsByChannelAndPhone(channelCode, phone);
        if (exists > 0) {
            throw new BusinessException("该号码在此通道已存在有效黑名单");
        }
        LocalDateTime expireTime = LocalDateTime.now().plusDays(DEFAULT_BLACKLIST_DAYS);
        Blacklist blacklist = new Blacklist()
                .setChannelCode(channelCode)
                .setPhone(phone)
                .setKeyword("MANUAL")
                .setSourceMoId(sourceMoId)
                .setExpireTime(expireTime);
        blacklistMapper.upsert(blacklist);
        // 同步写 Redis 缓存
        try {
            redisUtil.set(CACHE_KEY_PREFIX + channelCode + ":" + phone, "1", DEFAULT_BLACKLIST_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("加黑写入 Redis 缓存失败: {}", e.getMessage());
        }
        log.info("手动加黑成功: channel={}, phone={}, 过期时间={}", channelCode, phone, expireTime);
    }
}
