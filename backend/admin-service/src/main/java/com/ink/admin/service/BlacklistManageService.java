package com.ink.admin.service;

import com.ink.admin.entity.Blacklist;
import com.ink.admin.mapper.BlacklistMapper;
import com.ink.common.exception.BusinessException;
import com.ink.common.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 退订黑名单管理服务类
 * 提供分页查询与手动移除能力，移除时同步清理 api 侧黑名单缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlacklistManageService {

    /** 与 api 侧 RedisBlacklistChecker 保持一致的缓存 key 前缀 */
    private static final String CACHE_KEY_PREFIX = "ink:blacklist:";

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
}
