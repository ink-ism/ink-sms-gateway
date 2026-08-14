package com.ink.api.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.*;

/**
 * 计费服务
 * 预扣费 + 失败返还模式：Submit 提交前原子扣减余额，发送失败/黑名单拦截时返还
 *
 * 性能优化（v4）：
 * - tryDeduct / refund 纯内存操作（AtomicLong），耗时 ~0ms
 * - 定时 3s 批量同步余额到 Redis + MySQL（消除关键路径的远程调用）
 * - 流水写入完全异步化（不阻塞关键路径）
 * - findCostPrice 使用本地缓存（通道成本价变更低频）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final JdbcTemplate jdbcTemplate;
    private final SpAccountService spAccountService;
    private final StringRedisTemplate stringRedisTemplate;

    /** Redis 余额 key 前缀 */
    private static final String BALANCE_KEY_PREFIX = "ink:balance:";
    /** 余额精度：万分之一元（1 元 = 10000 单位） */
    private static final long BALANCE_SCALE = 10000L;
    /** Redis 余额 key TTL（兜底过期，防僵尸数据） */
    private static final long BALANCE_TTL_SECONDS = 3600L;

    /**
     * Redis Lua 脚本：原子检查余额并扣减（单次往返，避免多次 Redis 调用）
     * KEYS[1] = 余额 key
     * ARGV[1] = 扣减金额（万分之元）
     * ARGV[2] = MySQL 余额（万分之元，懒加载用）
     * ARGV[3] = TTL（秒）
     * 返回：扣减后余额（>=0 成功）；-1 = 余额不足；-2 = key 不存在且 MySQL 余额也为 0
     */
    private static final DefaultRedisScript<Long> DEDUCT_SCRIPT;
    static {
        DEDUCT_SCRIPT = new DefaultRedisScript<>();
        DEDUCT_SCRIPT.setScriptText(
            "local key = KEYS[1] " +
            "local amount = tonumber(ARGV[1]) " +
            "local mysqlBalance = tonumber(ARGV[2]) " +
            "local ttl = tonumber(ARGV[3]) " +
            "local current = redis.call('GET', key) " +
            "if not current then " +
            "  current = mysqlBalance " +
            "  redis.call('SET', key, current, 'EX', ttl) " +
            "end " +
            "local bal = tonumber(current) " +
            "if bal < amount then return -1 end " +
            "return redis.call('DECRBY', key, amount)"
        );
        DEDUCT_SCRIPT.setResultType(Long.class);
    }

    /** 流水异步写入线程池 */
    private final ExecutorService txExecutor = new ThreadPoolExecutor(
            1, 4, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(5000),
            r -> {
                Thread t = new Thread(r, "billing-tx-writer");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /** 本地余额计数器：spId -> AtomicLong（万分之元），关键路径零远程调用 */
    private final ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicLong> localBalance = new ConcurrentHashMap<>();

    /** 需要回写 Redis/MySQL 的 spId 集合 */
    private final ConcurrentLinkedQueue<String> dirtySpIds = new ConcurrentLinkedQueue<>();

    /** MySQL 余额内存缓存（spId -> 万分之元），供 Lua 脚本懒加载兜底 */
    private final ConcurrentHashMap<String, Long> mysqlBalanceCache = new ConcurrentHashMap<>();

    /** 通道成本价缓存（channelCode -> costPrice），定期刷新 */
    private final ConcurrentHashMap<String, BigDecimal> costPriceCache = new ConcurrentHashMap<>();
    private volatile long costPriceCacheExpire = 0;
    private static final long COST_PRICE_CACHE_TTL_MS = 60_000L; // 1 分钟

    @PostConstruct
    public void init() {
        // 预热：将所有启用客户的余额加载到 Redis
        warmUpBalances();
    }

    /**
     * 预热余额到本地内存（启动时从 MySQL 加载）
     */
    private void warmUpBalances() {
        try {
            jdbcTemplate.query("SELECT sp_id, balance FROM ink_sp WHERE status = 1",
                    (RowCallbackHandler) rs -> {
                        String spId = rs.getString("sp_id");
                        BigDecimal balance = rs.getBigDecimal("balance");
                        long units = toUnits(balance != null ? balance : BigDecimal.ZERO);
                        localBalance.put(spId, new java.util.concurrent.atomic.AtomicLong(units));
                        mysqlBalanceCache.put(spId, units);
                    });
            log.info("余额预热完成，加载 {} 个客户", localBalance.size());
        } catch (Exception e) {
            log.warn("余额预热失败（将在首次扣费时懒加载）: {}", e.getMessage());
        }
    }

    /**
     * 预扣费（纯内存原子操作）— 耗时 ~0ms
     * @param spId     客户标识
     * @param refMsgId 关联消息 ID（流水追溯用）
     * @return 扣费金额；客户不存在/已禁用/余额不足返回 null，单价为 0 时返回 ZERO
     */
    public BigDecimal tryDeduct(String spId, String refMsgId) {
        SpAccountService.SpAccount account = spAccountService.findAccount(spId);
        if (account == null) {
            return null;
        }
        BigDecimal amount = account.getUnitPrice();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // === 纯内存原子扣费（零远程调用） ===
        long amountUnits = toUnits(amount);
        java.util.concurrent.atomic.AtomicLong counter = localBalance.computeIfAbsent(spId,
                k -> new java.util.concurrent.atomic.AtomicLong(10000000000L)); // 默认 100万
        long newBal = counter.addAndGet(-amountUnits);
        if (newBal < 0) {
            counter.addAndGet(amountUnits); // rollback
            log.warn("SP 余额不足: spId={}, amount={}", spId, amount);
            return null;
        }
        dirtySpIds.offer(spId);

        // 流水异步写入
        final BigDecimal finalAmount = amount;
        final long finalBalance = newBal;
        txExecutor.execute(() -> {
            try {
                jdbcTemplate.update(
                        "INSERT INTO ink_sp_transaction (sp_id, type, amount, balance_after, ref_msg_id, remark) VALUES (?, ?, ?, ?, ?, ?)",
                        spId, "DEDUCT", finalAmount.negate(), fromUnits(finalBalance), refMsgId, "短信发送扣费");
            } catch (Exception e) {
                log.error("异步写入扣费流水失败: spId={}, error={}", spId, e.getMessage());
            }
        });
        return amount;
    }

    /**
     * 返还余额（纯内存原子操作）
     */
    public void refund(String spId, BigDecimal amount, String refMsgId) {
        if (spId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        long amountUnits = toUnits(amount);
        java.util.concurrent.atomic.AtomicLong counter = localBalance.computeIfAbsent(spId,
                k -> new java.util.concurrent.atomic.AtomicLong(0));
        long newBal = counter.addAndGet(amountUnits);
        dirtySpIds.offer(spId);
        log.debug("SP 余额已返还(内存): spId={}, amount={}, newBalance={}", spId, amount, fromUnits(newBal));

        // 流水异步写入
        txExecutor.execute(() -> {
            try {
                jdbcTemplate.update(
                        "INSERT INTO ink_sp_transaction (sp_id, type, amount, balance_after, ref_msg_id, remark) VALUES (?, ?, ?, ?, ?, ?)",
                        spId, "REFUND", amount, fromUnits(newBal), refMsgId, "发送失败返还");
            } catch (Exception e) {
                log.error("异步写入返还流水失败: spId={}, error={}", spId, e.getMessage());
            }
        });
    }

    /**
     * 查询通道成本价（带本地缓存，1 分钟 TTL）
     */
    public BigDecimal findCostPrice(String channelCode) {
        if (channelCode == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now > costPriceCacheExpire) {
            refreshCostPriceCache();
        }
        BigDecimal cached = costPriceCache.get(channelCode);
        if (cached != null) {
            return cached;
        }
        try {
            BigDecimal price = jdbcTemplate.queryForObject(
                    "SELECT cost_price FROM ink_channel WHERE code = ?",
                    BigDecimal.class, channelCode);
            if (price != null) {
                costPriceCache.put(channelCode, price);
            }
            return price;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 定时同步：本地余额 → Redis + MySQL ====================

    /**
     * 每 3 秒将本地余额同步到 Redis + MySQL（持久化）
     */
    @Scheduled(fixedDelay = 3000, initialDelay = 5000)
    public void syncBalancesToMysql() {
        // 去重
        java.util.Set<String> seen = new java.util.HashSet<>();
        String spId;
        while ((spId = dirtySpIds.poll()) != null) {
            seen.add(spId);
        }
        if (seen.isEmpty()) return;

        for (String sp : seen) {
            try {
                java.util.concurrent.atomic.AtomicLong counter = localBalance.get(sp);
                if (counter == null) continue;
                long units = counter.get();
                // 更新内存缓存
                mysqlBalanceCache.put(sp, units);
                // 同步到 Redis
                try {
                    String key = BALANCE_KEY_PREFIX + sp;
                    stringRedisTemplate.opsForValue().set(key, String.valueOf(units),
                            BALANCE_TTL_SECONDS, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.debug("同步余额到 Redis 失败: spId={}, error={}", sp, e.getMessage());
                }
                // 同步到 MySQL
                BigDecimal balance = fromUnits(units);
                jdbcTemplate.update("UPDATE ink_sp SET balance = ? WHERE sp_id = ?", balance, sp);
            } catch (Exception e) {
                log.warn("同步余额到 MySQL 失败: spId={}, error={}", sp, e.getMessage());
            }
        }
    }

    // ==================== 内部工具方法 ====================

    /**
     * 获取 MySQL 余额的内存缓存值（万分之元）
     * 启动时预热，运行中通过定时同步保持更新
     */
    private long getMysqlBalanceUnits(String spId) {
        Long cached = mysqlBalanceCache.get(spId);
        return cached != null ? cached : 0L;
    }

    /**
     * MySQL 降级扣费（Redis 不可用时使用）
     */
    private BigDecimal tryDeductMysql(String spId, BigDecimal amount, String refMsgId) {
        int rows = jdbcTemplate.update(
                "UPDATE ink_sp SET balance = balance - ? WHERE sp_id = ? AND status = 1 AND balance >= ?",
                amount, spId, amount);
        if (rows <= 0) {
            return null;
        }
        final BigDecimal finalAmount = amount;
        txExecutor.execute(() -> {
            try {
                BigDecimal balanceAfter = jdbcTemplate.queryForObject(
                        "SELECT balance FROM ink_sp WHERE sp_id = ?", BigDecimal.class, spId);
                jdbcTemplate.update(
                        "INSERT INTO ink_sp_transaction (sp_id, type, amount, balance_after, ref_msg_id, remark) VALUES (?, ?, ?, ?, ?, ?)",
                        spId, "DEDUCT", finalAmount.negate(), balanceAfter, refMsgId, "短信发送扣费(MySQL降级)");
            } catch (Exception e) {
                log.error("异步写入扣费流水失败: spId={}, error={}", spId, e.getMessage());
            }
        });
        return amount;
    }

    private void refreshCostPriceCache() {
        synchronized (this) {
            if (System.currentTimeMillis() <= costPriceCacheExpire) return;
            try {
                ConcurrentHashMap<String, BigDecimal> newCache = new ConcurrentHashMap<>();
                jdbcTemplate.query("SELECT code, cost_price FROM ink_channel WHERE cost_price IS NOT NULL",
                        (RowCallbackHandler) rs -> newCache.put(rs.getString("code"), rs.getBigDecimal("cost_price")));
                costPriceCache.clear();
                costPriceCache.putAll(newCache);
                costPriceCacheExpire = System.currentTimeMillis() + COST_PRICE_CACHE_TTL_MS;
            } catch (Exception e) {
                log.warn("刷新通道成本价缓存失败: {}", e.getMessage());
                costPriceCacheExpire = System.currentTimeMillis() + 10_000L;
            }
        }
    }

    /** 元 → 万分之一元 */
    private static long toUnits(BigDecimal yuan) {
        return yuan.multiply(BigDecimal.valueOf(BALANCE_SCALE)).longValue();
    }

    /** 万分之一元 → 元 */
    private static BigDecimal fromUnits(long units) {
        return BigDecimal.valueOf(units, 4);
    }

    @PreDestroy
    public void shutdown() {
        // 最终同步一次余额
        syncBalancesToMysql();
        txExecutor.shutdown();
        try {
            if (!txExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                txExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            txExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
