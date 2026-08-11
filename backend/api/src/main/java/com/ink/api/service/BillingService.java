package com.ink.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 计费服务
 * 预扣费 + 失败返还模式：Submit 提交前原子扣减余额，发送失败/黑名单拦截时返还
 * 扣费依赖 MySQL 单行原子 UPDATE，无需分布式锁
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final JdbcTemplate jdbcTemplate;
    private final SpAccountService spAccountService;

    /**
     * 预扣费（原子扣减）
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

        int rows = jdbcTemplate.update(
                "UPDATE ink_sp SET balance = balance - ? WHERE sp_id = ? AND status = 1 AND balance >= ?",
                amount, spId, amount);
        if (rows <= 0) {
            log.warn("SP 余额不足，扣费失败: spId={}, amount={}", spId, amount);
            return null;
        }

        recordTransaction(spId, "DEDUCT", amount.negate(), refMsgId, "短信发送扣费");
        return amount;
    }

    /**
     * 返还余额（发送失败/黑名单拦截时调用）
     */
    public void refund(String spId, BigDecimal amount, String refMsgId) {
        if (spId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        int rows = jdbcTemplate.update(
                "UPDATE ink_sp SET balance = balance + ? WHERE sp_id = ?",
                amount, spId);
        if (rows > 0) {
            recordTransaction(spId, "REFUND", amount, refMsgId, "发送失败返还");
            log.info("SP 余额已返还: spId={}, amount={}, refMsgId={}", spId, amount, refMsgId);
        }
    }

    /**
     * 查询通道成本价
     * @return 成本价；通道不存在返回 null
     */
    public BigDecimal findCostPrice(String channelCode) {
        if (channelCode == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT cost_price FROM ink_channel WHERE code = ?",
                    BigDecimal.class, channelCode);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 写入余额流水
     */
    private void recordTransaction(String spId, String type, BigDecimal amount, String refMsgId, String remark) {
        try {
            BigDecimal balanceAfter = jdbcTemplate.queryForObject(
                    "SELECT balance FROM ink_sp WHERE sp_id = ?", BigDecimal.class, spId);
            jdbcTemplate.update(
                    "INSERT INTO ink_sp_transaction (sp_id, type, amount, balance_after, ref_msg_id, remark) VALUES (?, ?, ?, ?, ?, ?)",
                    spId, type, amount, balanceAfter, refMsgId, remark);
        } catch (Exception e) {
            log.error("写入余额流水失败: spId={}, type={}, amount={}, error={}", spId, type, amount, e.getMessage(), e);
        }
    }
}
