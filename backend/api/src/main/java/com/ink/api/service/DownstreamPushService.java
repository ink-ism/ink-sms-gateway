package com.ink.api.service;

import com.ink.api.session.SpSessionManager;
import com.ink.channel.cmpp.CmppCommandType;
import com.ink.channel.cmpp.CmppMessage;
import com.ink.channel.cmpp.message.CmppDeliverRequestMessage;
import com.ink.channel.session.CmppSession;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 下游推送服务
 * 将状态报告/上行短信封装为 Deliver 推送给已认证的下游 SP；
 * SP 离线时写入 ink_push_queue 持久化队列，重连后补发
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DownstreamPushService {

    /** REST 客户标识（无 CMPP 长连接，不推送） */
    public static final String REST_SP_ID = "REST";

    /** 单客户待推消息上限，超出丢弃最旧 */
    private static final int MAX_QUEUE_PER_SP = 1000;

    /** 单次补发批量大小 */
    private static final int FLUSH_BATCH_SIZE = 200;

    private final SpSessionManager sessionManager;
    private final JdbcTemplate jdbcTemplate;

    /** Deliver 序列号生成器 */
    private final AtomicInteger seqGenerator = new AtomicInteger(1);

    /** Deliver Msg_Id 生成计数器 */
    private final AtomicLong msgIdCounter = new AtomicLong(0);

    /** 补发任务线程池（避免阻塞 Netty 线程） */
    private final ExecutorService flushExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sp-push-flush");
        t.setDaemon(true);
        return t;
    });

    // ==================== 推送入口 ====================

    /**
     * 推送状态报告给下游 SP（离线则入队）
     * @param destId      接入号（原下行的源号码）
     * @param reportMsgId 原下行 Submit 的服务端消息 ID
     */
    public void pushReport(String spId, String destId, long reportMsgId, String stat, String destPhone) {
        if (spId == null || REST_SP_ID.equals(spId)) {
            return;
        }
        CmppDeliverRequestMessage deliver =
                CmppDeliverRequestMessage.createReport(nextPushMsgId(), destId, reportMsgId, stat, destPhone);
        if (!trySend(spId, deliver)) {
            log.info("SP 离线，状态报告入队: spId={}, reportMsgId=0x{}", spId, Long.toHexString(reportMsgId));
            enqueue(spId, "REPORT", destPhone, null, Long.toHexString(reportMsgId), stat, 0);
        }
    }

    /**
     * 推送上行短信给下游 SP（离线则入队）
     */
    public void pushMo(String spId, String srcPhone, String destId, String content, int msgFmt) {
        if (spId == null || REST_SP_ID.equals(spId)) {
            return;
        }
        CmppDeliverRequestMessage deliver =
                CmppDeliverRequestMessage.createMo(nextPushMsgId(), srcPhone, destId, content, msgFmt);
        if (!trySend(spId, deliver)) {
            log.info("SP 离线，上行短信入队: spId={}, src={}", spId, srcPhone);
            enqueue(spId, "MO", srcPhone, content, null, null, msgFmt);
        }
    }

    /**
     * SP 认证成功后异步补发离线消息
     */
    public void flushQueueAsync(String spId) {
        flushExecutor.execute(() -> {
            try {
                flushQueue(spId);
            } catch (Exception e) {
                log.error("补发离线消息异常: spId={}, error={}", spId, e.getMessage(), e);
            }
        });
    }

    // ==================== 内部实现 ====================

    /**
     * 尝试在线推送
     */
    private boolean trySend(String spId, CmppDeliverRequestMessage deliver) {
        CmppSession session = sessionManager.getSession(spId);
        if (session == null || !session.isActive()) {
            return false;
        }
        int seqId = nextSeqId();
        CmppMessage message = CmppMessage.create(
                CmppCommandType.DELIVER.getCommandId(), seqId, deliver.toBytes());
        session.send(message);
        log.debug("已推送 Deliver: spId={}, seqId={}, isReport={}", spId, seqId, deliver.isReport());
        return true;
    }

    /**
     * 写入离线推送队列（含单客户上限保护）
     */
    private void enqueue(String spId, String msgType, String phone, String content,
                         String serverMsgId, String stat, int msgFmt) {
        try {
            Integer pendingCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ink_push_queue WHERE sp_id = ? AND status = 0",
                    Integer.class, spId);
            if (pendingCount != null && pendingCount >= MAX_QUEUE_PER_SP) {
                int overflow = pendingCount - MAX_QUEUE_PER_SP + 1;
                jdbcTemplate.update(
                        "DELETE FROM ink_push_queue WHERE id IN (SELECT id FROM (SELECT id FROM ink_push_queue " +
                        "WHERE sp_id = ? AND status = 0 ORDER BY id ASC LIMIT ?) t)", spId, overflow);
                log.warn("客户待推队列超限，丢弃最旧 {} 条: spId={}", overflow, spId);
            }
            jdbcTemplate.update(
                    "INSERT INTO ink_push_queue (sp_id, msg_type, phone, content, server_msg_id, stat, msg_fmt, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 0)",
                    spId, msgType, phone, content, serverMsgId, stat, msgFmt);
        } catch (Exception e) {
            log.error("写入离线推送队列失败: spId={}, error={}", spId, e.getMessage(), e);
        }
    }

    /**
     * 补发指定客户的待推消息，SP 再次离线时中止
     */
    private void flushQueue(String spId) {
        List<Map<String, Object>> pending = jdbcTemplate.queryForList(
                "SELECT id, msg_type, phone, content, server_msg_id, stat, msg_fmt FROM ink_push_queue " +
                "WHERE sp_id = ? AND status = 0 ORDER BY id ASC LIMIT ?", spId, FLUSH_BATCH_SIZE);
        if (pending.isEmpty()) {
            return;
        }
        log.info("开始补发离线消息: spId={}, count={}", spId, pending.size());
        int pushed = 0;
        for (Map<String, Object> row : pending) {
            Long id = ((Number) row.get("id")).longValue();
            String msgType = (String) row.get("msg_type");
            String phone = (String) row.get("phone");
            CmppDeliverRequestMessage deliver;
            if ("REPORT".equals(msgType)) {
                String serverMsgIdHex = (String) row.get("server_msg_id");
                String srcId = findSrcIdByServerMsgId(serverMsgIdHex);
                long reportMsgId = parseMsgIdHex(serverMsgIdHex);
                deliver = CmppDeliverRequestMessage.createReport(
                        nextPushMsgId(), srcId, reportMsgId, (String) row.get("stat"), phone);
            } else {
                int msgFmt = row.get("msg_fmt") != null ? ((Number) row.get("msg_fmt")).intValue() : 8;
                deliver = CmppDeliverRequestMessage.createMo(
                        nextPushMsgId(), phone, null, (String) row.get("content"), msgFmt);
            }
            if (!trySend(spId, deliver)) {
                // SP 再次离线，中止补发等待下次重连
                jdbcTemplate.update("UPDATE ink_push_queue SET retry_count = retry_count + 1 WHERE id = ?", id);
                log.info("补发中止（SP 离线）: spId={}, 已补发 {} 条", spId, pushed);
                return;
            }
            jdbcTemplate.update("UPDATE ink_push_queue SET status = 1 WHERE id = ?", id);
            pushed++;
        }
        log.info("离线消息补发完成: spId={}, count={}", spId, pushed);
        // 批量处理完毕仍有剩余则继续
        Integer remain = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ink_push_queue WHERE sp_id = ? AND status = 0", Integer.class, spId);
        if (remain != null && remain > 0) {
            flushQueue(spId);
        }
    }

    /**
     * 查询原下行的源号码（接入号），补发报告时作为 Dest_Id
     */
    private String findSrcIdByServerMsgId(String serverMsgIdHex) {
        try {
            String srcId = jdbcTemplate.queryForObject(
                    "SELECT src_id FROM ink_sms_down WHERE server_msg_id = ? ORDER BY create_time DESC LIMIT 1",
                    String.class, serverMsgIdHex);
            return srcId != null ? srcId : "10690000";
        } catch (Exception e) {
            return "10690000";
        }
    }

    private long parseMsgIdHex(String hex) {
        try {
            return Long.parseUnsignedLong(hex, 16);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 生成推送 Deliver 的 Msg_Id（时间戳基，与上游 Msg_Id 无对应关系）
     */
    private long nextPushMsgId() {
        return System.currentTimeMillis() * 10000 + (msgIdCounter.incrementAndGet() % 10000);
    }

    private int nextSeqId() {
        int seq = seqGenerator.getAndIncrement();
        if (seq > 0xFFFFFF) {
            seqGenerator.set(1);
            return 1;
        }
        return seq;
    }

    @PreDestroy
    public void shutdown() {
        flushExecutor.shutdown();
        try {
            if (!flushExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                flushExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            flushExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
