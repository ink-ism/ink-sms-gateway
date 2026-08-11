package com.ink.api.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 下游客户账号服务
 * 从 ink_sp 表加载启用客户快照（密钥/单价/限速），供 CMPP Connect 认证、限流判定使用
 * 注意：balance 仅作展示参考，扣费以 BillingService 的 DB 原子更新为准
 */
@Slf4j
@Service
@RequiredArgsConstructor
@DependsOn("smsRecordService")
public class SpAccountService {

    private final JdbcTemplate jdbcTemplate;

    /** 客户账号快照 */
    @Getter
    public static class SpAccount {
        private final String secret;
        private final BigDecimal unitPrice;
        private final int rateLimit;

        public SpAccount(String secret, BigDecimal unitPrice, int rateLimit) {
            this.secret = secret;
            this.unitPrice = unitPrice;
            this.rateLimit = rateLimit;
        }
    }

    /** 客户账号映射：spId -> SpAccount */
    private volatile Map<String, SpAccount> accountMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * 查询客户密钥
     * @return secret；客户不存在或已禁用返回 null
     */
    public String findSecret(String spId) {
        SpAccount account = findAccount(spId);
        return account != null ? account.getSecret() : null;
    }

    /**
     * 查询客户账号快照
     * @return 账号快照；客户不存在或已禁用返回 null
     */
    public SpAccount findAccount(String spId) {
        if (spId == null) {
            return null;
        }
        return accountMap.get(spId);
    }

    /**
     * 从数据库重新加载启用客户
     */
    public synchronized void reload() {
        Map<String, SpAccount> newMap = new ConcurrentHashMap<>();
        try {
            jdbcTemplate.query("SELECT sp_id, sp_secret, unit_price, rate_limit FROM ink_sp WHERE status = 1", rs -> {
                BigDecimal unitPrice = rs.getBigDecimal("unit_price");
                newMap.put(rs.getString("sp_id"), new SpAccount(
                        rs.getString("sp_secret"),
                        unitPrice != null ? unitPrice : BigDecimal.valueOf(0.05),
                        rs.getInt("rate_limit")));
            });
            accountMap = newMap;
            log.info("已加载 {} 个启用下游客户", newMap.size());
        } catch (Exception e) {
            log.error("加载下游客户失败: {}", e.getMessage(), e);
        }
    }
}
