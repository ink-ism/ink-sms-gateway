package com.ink.api.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.*;

/**
 * 短信记录服务
 * 使用异步线程池写入数据库，避免阻塞请求线程
 */
@Slf4j
@Service
public class SmsRecordService {

    private final JdbcTemplate jdbcTemplate;

    /** 异步写入线程池（有界队列，防止 OOM） */
    private final ExecutorService dbWriteExecutor;

    public SmsRecordService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.dbWriteExecutor = new ThreadPoolExecutor(
                2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                r -> {
                    Thread t = new Thread(r, "sms-db-writer");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()  // 队列满时降级为同步写入
        );
    }

    @PostConstruct
    public void initSchema() {
        try {
            // 检查 server_msg_id 列是否存在，不存在则添加
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ink_sms_down' AND COLUMN_NAME = 'server_msg_id'",
                    Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE ink_sms_down ADD COLUMN server_msg_id VARCHAR(32) DEFAULT NULL AFTER msg_id");
                log.info("已添加 server_msg_id 列到 ink_sms_down 表");
            }
            // 确保表使用 utf8mb4 字符集
            jdbcTemplate.execute("ALTER TABLE ink_sms_down CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            log.info("已确保 ink_sms_down 表使用 utf8mb4 字符集");
        } catch (Exception e) {
            log.warn("检查/添加 schema 失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 异步记录下行短信
     * @param clientMsgId 客户端生成的唯一消息 ID（作为数据库主键）
     * @param serverMsgId CMPP 服务端返回的消息 ID（用于状态报告匹配）
     */
    public void recordSmsDown(String clientMsgId, String serverMsgId, String srcId, String destTerminalId,
                              String msgContent, int msgFmt, String serviceId,
                              String channelCode, int status, String errorMsg) {
        dbWriteExecutor.execute(() -> doRecordSmsDown(clientMsgId, serverMsgId, srcId, destTerminalId,
                msgContent, msgFmt, serviceId, channelCode, status, errorMsg));
    }

    /**
     * 异步记录上行短信
     */
    public void recordSmsUp(String msgId, String srcTerminalId, String destId,
                            String msgContent, int msgFmt, String serviceId,
                            String channelCode) {
        dbWriteExecutor.execute(() -> doRecordSmsUp(msgId, srcTerminalId, destId,
                msgContent, msgFmt, serviceId, channelCode));
    }

    /**
     * 异步更新下行短信状态报告
     * @param serverMsgId CMPP 服务端返回的消息 ID（状态报告中的 reportMsgId）
     */
    public void updateSmsDownStatus(String serverMsgId, String destTerminalId, String reportStat) {
        dbWriteExecutor.execute(() -> doUpdateSmsDownStatus(serverMsgId, destTerminalId, reportStat));
    }

    // ==================== 实际 DB 操作（在异步线程中执行） ====================

    private void doRecordSmsDown(String clientMsgId, String serverMsgId, String srcId, String destTerminalId,
                                 String msgContent, int msgFmt, String serviceId,
                                 String channelCode, int status, String errorMsg) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO ink_sms_down (msg_id, server_msg_id, src_id, dest_terminal_id, msg_content, msg_fmt, service_id, channel_code, status, error_msg, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    clientMsgId, serverMsgId, srcId, destTerminalId, msgContent, msgFmt, serviceId, channelCode, status, errorMsg, LocalDateTime.now()
            );
            log.debug("下行短信记录已写入: clientMsgId={}, serverMsgId={}, dest={}", clientMsgId, serverMsgId, destTerminalId);
        } catch (Exception e) {
            log.error("写入下行短信记录失败: {}", e.getMessage(), e);
        }
    }

    private void doRecordSmsUp(String msgId, String srcTerminalId, String destId,
                               String msgContent, int msgFmt, String serviceId,
                               String channelCode) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO ink_sms_up (msg_id, src_terminal_id, dest_id, msg_content, msg_fmt, service_id, is_report, report_stat, channel_code, create_time) VALUES (?, ?, ?, ?, ?, ?, 0, NULL, ?, ?)",
                    msgId, srcTerminalId, destId, msgContent, msgFmt, serviceId, channelCode, LocalDateTime.now()
            );
            log.debug("上行短信记录已写入: msgId={}, src={}", msgId, srcTerminalId);
        } catch (Exception e) {
            log.error("写入上行短信记录失败: {}", e.getMessage(), e);
        }
    }

    private void doUpdateSmsDownStatus(String serverMsgId, String destTerminalId, String reportStat) {
        try {
            int status;
            if ("DELIVRD".equalsIgnoreCase(reportStat)) {
                status = 3; // 发送成功
            } else if ("ACCEPTED".equalsIgnoreCase(reportStat)) {
                status = 1; // 已提交
            } else {
                status = 2; // 发送失败
            }
            // 通过 server_msg_id 查找对应的 client msg_id，再更新状态
            String clientMsgId = jdbcTemplate.queryForObject(
                    "SELECT msg_id FROM ink_sms_down WHERE server_msg_id = ? ORDER BY create_time DESC LIMIT 1",
                    String.class, serverMsgId
            );
            if (clientMsgId != null) {
                int rows = jdbcTemplate.update(
                        "UPDATE ink_sms_down SET status = ?, status_report = ? WHERE msg_id = ?",
                        status, reportStat, clientMsgId
                );
                if (rows > 0) {
                    log.debug("下行短信状态已更新: clientMsgId={}, serverMsgId={}, stat={}, status={}",
                            clientMsgId, serverMsgId, reportStat, status);
                }
            } else {
                log.warn("未找到对应的下行记录: serverMsgId={}", serverMsgId);
            }
        } catch (Exception e) {
            log.error("更新下行短信状态失败: serverMsgId={}, error={}", serverMsgId, e.getMessage(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        dbWriteExecutor.shutdown();
        try {
            if (!dbWriteExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                dbWriteExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            dbWriteExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("短信记录异步写入线程池已关闭");
    }
}
