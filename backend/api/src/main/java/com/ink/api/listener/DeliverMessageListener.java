package com.ink.api.listener;

import com.ink.api.config.CmppUnsubscribeConfig;
import com.ink.api.service.BlacklistService;
import com.ink.api.service.DownstreamPushService;
import com.ink.api.service.SmsRecordService;
import com.ink.channel.cmpp.CmppCommandType;
import com.ink.channel.cmpp.CmppMessage;
import com.ink.channel.cmpp.message.CmppDeliverRequestMessage;
import com.ink.common.utils.PhoneCarrierUtil;
import com.ink.core.connection.CmppConnectionManager;
import com.ink.core.handler.CmppMessageHandler;
import com.ink.core.repository.SubmitRouteRepository;
import com.ink.core.routing.MsgRouteRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Deliver 消息监听器
 * 监听上游 CMPP 服务器推送的 Deliver 消息（状态报告、上行短信）：
 * - 状态报告：更新下行记录状态，并按发送客户路由推送 Deliver 回执（REST 客户仅落库）
 * - 上行短信：命中退订关键字则登记通道级黑名单；否则路由给最近发送客户并推送
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliverMessageListener {

    private final CmppConnectionManager connectionManager;
    private final SmsRecordService smsRecordService;
    private final MsgRouteRegistry routeRegistry;
    private final SubmitRouteRepository submitRouteRepository;
    private final CmppUnsubscribeConfig unsubscribeConfig;
    private final BlacklistService blacklistService;
    private final DownstreamPushService pushService;

    /** Deliver 处理线程池：避免阻塞 Netty IO 线程 */
    private final ExecutorService deliverExecutor = new ThreadPoolExecutor(
            1, 2, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2000),
            r -> {
                Thread t = new Thread(r, "deliver-handler");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @PostConstruct
    public void init() {
        connectionManager.setMessageHandler(new CmppMessageHandler() {
            @Override
            public void handleMessage(CmppMessage message, String channelCode) {
                try {
                    int commandId = message.getCommandId();
                    CmppCommandType cmdType = CmppCommandType.fromCommandId(commandId);
                    if (cmdType != CmppCommandType.DELIVER) {
                        return;
                    }
                    CmppDeliverRequestMessage deliver = CmppDeliverRequestMessage.fromBytes(message.getBody());
                    deliverExecutor.execute(() -> handleDeliver(deliver, channelCode));
                } catch (Exception e) {
                    log.error("处理 Deliver 消息异常: {}", e.getMessage(), e);
                }
            }

            @Override
            public void onConnected() {
                log.info("CMPP 上游连接已建立");
            }

            @Override
            public void onDisconnected() {
                log.info("CMPP 上游连接已断开");
            }
        });
        log.info("Deliver 消息监听器已注册");
    }

    /**
     * 处理单条 Deliver（在独立线程中执行）
     */
    private void handleDeliver(CmppDeliverRequestMessage deliver, String channelCode) {
        try {
            if (deliver.isReport()) {
                handleReport(deliver);
            } else {
                handleMo(deliver, channelCode);
            }
        } catch (Exception e) {
            log.error("Deliver 业务处理异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 状态报告：更新下行状态 + 路由推送给发送客户
     */
    private void handleReport(CmppDeliverRequestMessage deliver) {
        String reportMsgIdHex = Long.toHexString(deliver.getReportMsgId());
        String destTerminalId = deliver.getSrcTerminalId();
        String stat = deliver.getReportStat();
        log.info("收到状态报告: msgId=0x{}, reportMsgId=0x{}, stat={}, srcTerminal={}",
                Long.toHexString(deliver.getMsgId()), reportMsgIdHex, stat, destTerminalId);

        // 1. 路由发送客户：内存注册表优先，DB 兜底（重启后迟到回执）
        MsgRouteRegistry.RouteEntry entry = routeRegistry.lookup(reportMsgIdHex);
        String spId = entry != null ? entry.getSpId() : submitRouteRepository.findSpIdByServerMsgId(reportMsgIdHex);

        // 2. 更新下行记录状态
        smsRecordService.updateSmsDownStatus(reportMsgIdHex, destTerminalId, stat);

        // 3. 推送回执给发送客户（REST 客户仅落库不推送）
        if (spId == null) {
            log.warn("状态报告未找到发送客户，仅落库: reportMsgId=0x{}", reportMsgIdHex);
            return;
        }
        if (DownstreamPushService.REST_SP_ID.equals(spId)) {
            return;
        }
        String srcId = smsRecordService.findSrcIdByServerMsgId(reportMsgIdHex);
        pushService.pushReport(spId, srcId, deliver.getReportMsgId(), stat, destTerminalId);
    }

    /**
     * 上行短信：退订关键字判定 + 路由推送给最近发送客户
     */
    private void handleMo(CmppDeliverRequestMessage deliver, String channelCode) {
        String msgIdStr = Long.toHexString(deliver.getMsgId());
        String phone = deliver.getSrcTerminalId();
        String destId = deliver.getDestId();
        String content = deliver.getMsgContent();
        log.info("收到上行短信: msgId=0x{}, channel={}, src={}, dest={}, content={}",
                msgIdStr, channelCode, phone, destId, content);

        // 1. 退订关键字命中：登记通道级黑名单，不推送给下游
        String keyword = unsubscribeConfig.matchKeyword(content);
        if (keyword != null) {
            log.info("命中退订关键字，登记黑名单: channel={}, phone={}, keyword={}", channelCode, phone, keyword);
            blacklistService.addByUnsubscribe(channelCode, phone, keyword, msgIdStr);
            smsRecordService.recordSmsUp(msgIdStr, null, phone, destId, content,
                    deliver.getMsgFmt(), deliver.getServiceId(), channelCode, PhoneCarrierUtil.resolve(phone));
            return;
        }

        // 2. 路由给最近一次向该号码发送的客户
        String spId = smsRecordService.findLatestSpIdByPhone(phone);
        smsRecordService.recordSmsUp(msgIdStr, spId, phone, destId, content,
                deliver.getMsgFmt(), deliver.getServiceId(), channelCode, PhoneCarrierUtil.resolve(phone));
        if (spId == null) {
            log.info("上行短信未找到路由客户，仅落库: src={}", phone);
            return;
        }
        pushService.pushMo(spId, phone, destId, content, deliver.getMsgFmt());
    }

    @PreDestroy
    public void shutdown() {
        deliverExecutor.shutdown();
        try {
            if (!deliverExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                deliverExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            deliverExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
