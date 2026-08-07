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
            // 检查列是否存在，不存在则添加（兼容存量库）
            ensureColumn("ink_sms_down", "server_msg_id", "VARCHAR(32) DEFAULT NULL AFTER msg_id");
            ensureColumn("ink_sms_down", "sp_id", "VARCHAR(30) DEFAULT NULL AFTER server_msg_id");
            ensureColumn("ink_sms_up", "sp_id", "VARCHAR(30) DEFAULT NULL AFTER msg_id");
            ensureIndex("ink_sms_down", "idx_server_msg_id", "server_msg_id");
            ensureIndex("ink_sms_down", "idx_sp_id", "sp_id");

            // 新链路表：客户、客户-通道绑定、黑名单、离线推送队列
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS ink_sp (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "sp_id VARCHAR(30) NOT NULL UNIQUE COMMENT '客户标识'," +
                    "sp_secret VARCHAR(100) NOT NULL COMMENT '共享密钥'," +
                    "name VARCHAR(50) NOT NULL COMMENT '客户名称'," +
                    "status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用'," +
                    "description VARCHAR(255) DEFAULT NULL COMMENT '客户描述'," +
                    "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "INDEX idx_status (status)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='下游客户表'");
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS ink_sp_channel (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "sp_id VARCHAR(30) NOT NULL," +
                    "channel_code VARCHAR(30) NOT NULL," +
                    "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "UNIQUE KEY uk_sp_channel (sp_id, channel_code)," +
                    "INDEX idx_sp_id (sp_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户通道绑定表'");
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS ink_blacklist (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "channel_code VARCHAR(30) NOT NULL," +
                    "phone VARCHAR(21) NOT NULL," +
                    "keyword VARCHAR(50) DEFAULT NULL," +
                    "source_mo_id VARCHAR(64) DEFAULT NULL," +
                    "expire_time DATETIME NOT NULL," +
                    "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "UNIQUE KEY uk_channel_phone (channel_code, phone)," +
                    "INDEX idx_expire_time (expire_time)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道级退订黑名单表'");
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS ink_push_queue (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "sp_id VARCHAR(30) NOT NULL," +
                    "msg_type VARCHAR(10) NOT NULL," +
                    "phone VARCHAR(21) DEFAULT NULL," +
                    "content TEXT DEFAULT NULL," +
                    "server_msg_id VARCHAR(32) DEFAULT NULL," +
                    "stat VARCHAR(20) DEFAULT NULL," +
                    "msg_fmt INT DEFAULT 8," +
                    "status TINYINT DEFAULT 0," +
                    "retry_count INT DEFAULT 0," +
                    "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "INDEX idx_sp_status (sp_id, status)," +
                    "INDEX idx_create_time (create_time)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='下游离线推送队列表'");

            // 确保表使用 utf8mb4 字符集
            jdbcTemplate.execute("ALTER TABLE ink_sms_down CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            log.info("已确保 ink_sms_down 表使用 utf8mb4 字符集");
        } catch (Exception e) {
            log.warn("检查/添加 schema 失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 确保表存在指定列，不存在则 ALTER 添加
     */
    private void ensureColumn(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            log.info("已添加 {} 列到 {} 表", column, table);
        }
    }

    /**
     * 确保表存在指定索引，不存在则创建
     */
    private void ensureIndex(String table, String indexName, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class, table, indexName);
        if (count != null && count == 0) {
            jdbcTemplate.execute("CREATE INDEX " + indexName + " ON " + table + " (" + column + ")");
            log.info("已创建索引 {} 于 {} 表", indexName, table);
        }
    }

    /**
     * 异步记录下行短信
     * @param clientMsgId 客户端生成的唯一消息 ID（作为数据库主键）
     * @param serverMsgId CMPP 服务端返回的消息 ID（用于状态报告匹配）
     * @param spId        发送客户标识（REST 发送为 "REST"）
     */
    public void recordSmsDown(String clientMsgId, String serverMsgId, String spId, String srcId, String destTerminalId,
                              String msgContent, int msgFmt, String serviceId,
                              String channelCode, int status, String errorMsg) {
        dbWriteExecutor.execute(() -> doRecordSmsDown(clientMsgId, serverMsgId, spId, srcId, destTerminalId,
                msgContent, msgFmt, serviceId, channelCode, status, errorMsg));
    }

    /**
     * 异步记录上行短信
     * @param spId 路由目标客户标识（可为 null）
     */
    public void recordSmsUp(String msgId, String spId, String srcTerminalId, String destId,
                            String msgContent, int msgFmt, String serviceId,
                            String channelCode) {
        dbWriteExecutor.execute(() -> doRecordSmsUp(msgId, spId, srcTerminalId, destId,
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

    private void doRecordSmsDown(String clientMsgId, String serverMsgId, String spId, String srcId, String destTerminalId,
                                 String msgContent, int msgFmt, String serviceId,
                                 String channelCode, int status, String errorMsg) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO ink_sms_down (msg_id, server_msg_id, sp_id, src_id, dest_terminal_id, msg_content, msg_fmt, service_id, channel_code, status, error_msg, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    clientMsgId, serverMsgId, spId, srcId, destTerminalId, msgContent, msgFmt, serviceId, channelCode, status, errorMsg, LocalDateTime.now()
            );
            log.debug("下行短信记录已写入: clientMsgId={}, serverMsgId={}, spId={}, dest={}", clientMsgId, serverMsgId, spId, destTerminalId);
        } catch (Exception e) {
            log.error("写入下行短信记录失败: {}", e.getMessage(), e);
        }
    }

    private void doRecordSmsUp(String msgId, String spId, String srcTerminalId, String destId,
                               String msgContent, int msgFmt, String serviceId,
                               String channelCode) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO ink_sms_up (msg_id, sp_id, src_terminal_id, dest_id, msg_content, msg_fmt, service_id, is_report, report_stat, channel_code, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, 0, NULL, ?, ?)",
                    msgId, spId, srcTerminalId, destId, msgContent, msgFmt, serviceId, channelCode, LocalDateTime.now()
            );
            log.debug("上行短信记录已写入: msgId={}, spId={}, src={}", msgId, spId, srcTerminalId);
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

    /**
     * 查询最近一次向指定手机号发送的客户标识（MO 路由用）
     */
    public String findLatestSpIdByPhone(String phone) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT sp_id FROM ink_sms_down WHERE dest_terminal_id = ? AND sp_id IS NOT NULL ORDER BY create_time DESC, id DESC LIMIT 1",
                    String.class, phone);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 按服务端消息 ID 查询原下行的源号码（回执推送的 Dest_Id）
     */
    public String findSrcIdByServerMsgId(String serverMsgIdHex) {
        try {
            String srcId = jdbcTemplate.queryForObject(
                    "SELECT src_id FROM ink_sms_down WHERE server_msg_id = ? ORDER BY create_time DESC LIMIT 1",
                    String.class, serverMsgIdHex);
            return srcId != null ? srcId : "10690000";
        } catch (Exception e) {
            return "10690000";
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
