package com.ink.api.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 下游客户账号服务
 * 从 ink_sp 表加载启用客户（spId -> secret），供 CMPP Connect 认证使用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpAccountService {

    private final JdbcTemplate jdbcTemplate;

    /** 客户密钥映射：spId -> secret */
    private volatile Map<String, String> secretMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * 查询客户密钥
     * @return secret；客户不存在或已禁用返回 null
     */
    public String findSecret(String spId) {
        if (spId == null) {
            return null;
        }
        return secretMap.get(spId);
    }

    /**
     * 从数据库重新加载启用客户
     */
    public synchronized void reload() {
        Map<String, String> newMap = new ConcurrentHashMap<>();
        try {
            jdbcTemplate.query("SELECT sp_id, sp_secret FROM ink_sp WHERE status = 1", rs -> {
                newMap.put(rs.getString("sp_id"), rs.getString("sp_secret"));
            });
            secretMap = newMap;
            log.info("已加载 {} 个启用下游客户", newMap.size());
        } catch (Exception e) {
            log.error("加载下游客户失败: {}", e.getMessage(), e);
        }
    }
}
