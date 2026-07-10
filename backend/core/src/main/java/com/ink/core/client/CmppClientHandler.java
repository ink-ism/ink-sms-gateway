package com.ink.core.client;

import com.ink.channel.cmpp.CmppCommandType;
import com.ink.channel.cmpp.CmppConstants;
import com.ink.channel.cmpp.CmppMessage;
import com.ink.channel.cmpp.codec.CmppPacketDecoder;
import com.ink.channel.cmpp.codec.CmppPacketEncoder;
import com.ink.channel.cmpp.message.CmppConnectRequestMessage;
import com.ink.channel.cmpp.message.CmppConnectResponseMessage;
import com.ink.channel.cmpp.message.CmppActiveTestRequestMessage;
import com.ink.channel.cmpp.message.CmppActiveTestResponseMessage;
import com.ink.channel.cmpp.message.CmppDeliverRequestMessage;
import com.ink.channel.cmpp.message.CmppDeliverResponseMessage;
import com.ink.channel.cmpp.message.CmppSubmitResponseMessage;
import com.ink.channel.cmpp.message.CmppTerminateRequestMessage;
import com.ink.channel.session.CmppSession;
import com.ink.core.config.CmppChannelConfig;
import com.ink.core.handler.CmppMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CMPP 客户端消息处理器
 * 处理认证、心跳、Deliver 等消息
 */
public class CmppClientHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(CmppClientHandler.class);

    private final CmppChannelConfig config;
    private final CmppMessageHandler messageHandler;
    private final AtomicInteger sequenceGenerator = new AtomicInteger(CmppConstants.SEQUENCE_START);
    private CmppSession session;

    /** 等待 Submit 响应的 Future 映射 (seqId -> Future<serverMsgId>) */
    private final ConcurrentHashMap<Integer, CompletableFuture<Long>> pendingSubmits = new ConcurrentHashMap<>();

    public CmppClientHandler(CmppChannelConfig config, CmppMessageHandler messageHandler) {
        this.config = config;
        this.messageHandler = messageHandler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("CMPP 连接已建立: {}:{}", config.getHost(), config.getPort());
        session = new CmppSession(ctx.channel(), config.getSpId());

        // 发送 Connect 请求进行认证
        sendConnectRequest(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof CmppMessage cmppMsg)) {
            return;
        }

        int commandId = cmppMsg.getCommandId();
        CmppCommandType cmdType = CmppCommandType.fromCommandId(commandId);

        if (cmdType == null) {
            log.warn("未知的 CMPP 命令: 0x{}", String.format("%08X", commandId));
            return;
        }

        switch (cmdType) {
            case CONNECT_RESP -> handleConnectResponse(cmppMsg, ctx);
            case SUBMIT_RESP -> handleSubmitResponse(cmppMsg);
            case ACTIVE_TEST_RESP -> handleActiveTestResponse(cmppMsg);
            case DELIVER -> handleDeliverRequest(cmppMsg, ctx);
            case TERMINATE_RESP -> handleTerminateResponse(cmppMsg);
            default -> log.debug("收到 CMPP 消息: {}", cmdType.getDescription());
        }
    }

    /**
     * 发送 Connect 认证请求
     */
    private void sendConnectRequest(ChannelHandlerContext ctx) {
        CmppConnectRequestMessage connectReq = CmppConnectRequestMessage.create(
                config.getSpId(), config.getSharedSecret(), (byte) config.getVersion());

        int seqId = nextSequenceId();
        CmppMessage message = CmppMessage.create(
                CmppCommandType.CONNECT.getCommandId(), seqId, connectReq.toBytes());

        log.info("发送 CMPP Connect 请求: spId={}, seqId={}", config.getSpId(), seqId);
        ctx.writeAndFlush(message);
    }

    /**
     * 处理 Connect 响应
     */
    private void handleConnectResponse(CmppMessage cmppMsg, ChannelHandlerContext ctx) {
        CmppConnectResponseMessage resp = CmppConnectResponseMessage.fromBytes(cmppMsg.getBody());

        if (resp.isSuccess()) {
            log.info("CMPP Connect 认证成功");
            session.markAuthenticated();
            messageHandler.onConnected();
        } else {
            log.error("CMPP Connect 认证失败: status={}", resp.getStatus());
            ctx.close();
        }
    }

    /**
     * 处理 Submit 响应，提取服务器分配的 Msg_Id
     */
    private void handleSubmitResponse(CmppMessage cmppMsg) {
        int seqId = cmppMsg.getSequenceId();
        CmppSubmitResponseMessage resp = CmppSubmitResponseMessage.fromBytes(cmppMsg.getBody());

        CompletableFuture<Long> future = pendingSubmits.remove(seqId);
        if (future != null) {
            if (resp.isSuccess()) {
                log.info("Submit 响应成功: seqId={}, serverMsgId=0x{}", seqId, Long.toHexString(resp.getMsgId()));
                future.complete(resp.getMsgId());
            } else {
                log.warn("Submit 响应失败: seqId={}, result={}", seqId, resp.getResult());
                future.completeExceptionally(new RuntimeException("Submit 失败, result=" + resp.getResult()));
            }
        } else {
            log.warn("收到未知的 Submit 响应: seqId={}", seqId);
        }
    }

    /**
     * 注册等待 Submit 响应的 Future
     */
    public void registerPendingSubmit(int seqId, CompletableFuture<Long> future) {
        pendingSubmits.put(seqId, future);
    }

    /**
     * 处理心跳响应
     */
    private void handleActiveTestResponse(CmppMessage cmppMsg) {
        CmppActiveTestResponseMessage resp = CmppActiveTestResponseMessage.fromBytes(cmppMsg.getBody());
        if (resp.isSuccess()) {
            session.updateActiveTime();
            log.debug("心跳响应正常");
        } else {
            log.warn("心跳响应异常: errorCode={}", resp.getErrorCode());
        }
    }

    /**
     * 处理 Deliver 请求（短信下发/状态报告）
     */
    private void handleDeliverRequest(CmppMessage cmppMsg, ChannelHandlerContext ctx) {
        CmppDeliverRequestMessage deliver = CmppDeliverRequestMessage.fromBytes(cmppMsg.getBody());

        if (deliver.isReport()) {
            log.info("收到短信状态报告: msgId=0x{}, reportMsgId=0x{}, stat={}, srcTerminal={}",
                    Long.toHexString(deliver.getMsgId()),
                    Long.toHexString(deliver.getReportMsgId()),
                    deliver.getReportStat(),
                    deliver.getSrcTerminalId());
        } else {
            log.info("收到上行短信: srcTerminal={}, destId={}, content={}",
                    deliver.getSrcTerminalId(), deliver.getDestId(), deliver.getMsgContent());
        }

        // 发送 Deliver 响应
        CmppDeliverResponseMessage deliverResp = new CmppDeliverResponseMessage();
        deliverResp.setMsgId(deliver.getMsgId());
        deliverResp.setResult(0); // 成功

        int seqId = cmppMsg.getSequenceId();
        CmppMessage response = CmppMessage.create(
                CmppCommandType.DELIVER_RESP.getCommandId(), seqId, deliverResp.toBytes());
        ctx.writeAndFlush(response);

        // 回调处理
        messageHandler.handleMessage(cmppMsg);
    }

    /**
     * 处理 Terminate 响应
     */
    private void handleTerminateResponse(CmppMessage cmppMsg) {
        log.info("收到 Terminate 响应，连接即将关闭");
    }

    /**
     * 发送心跳
     */
    public void sendHeartbeat() {
        if (session != null && session.isAuthenticated()) {
            int seqId = nextSequenceId();
            CmppMessage message = CmppMessage.create(
                    CmppCommandType.ACTIVE_TEST.getCommandId(), seqId,
                    new CmppActiveTestRequestMessage().toBytes());
            session.send(message);
            log.debug("发送心跳: seqId={}", seqId);
        }
    }

    /**
     * 发送 Terminate 断开连接
     */
    public void sendTerminate() {
        if (session != null && session.isAuthenticated()) {
            int seqId = nextSequenceId();
            CmppMessage message = CmppMessage.create(
                    CmppCommandType.TERMINATE.getCommandId(), seqId,
                    new CmppTerminateRequestMessage().toBytes());
            session.send(message);
            log.info("发送 Terminate 请求");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("CMPP 连接断开: {}:{}", config.getHost(), config.getPort());
        if (session != null) {
            session.close();
        }
        messageHandler.onDisconnected();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("CMPP 连接异常: {}", cause.getMessage(), cause);
        ctx.close();
    }

    public CmppSession getSession() {
        return session;
    }

    private int nextSequenceId() {
        int seq = sequenceGenerator.getAndIncrement();
        if (seq > CmppConstants.SEQUENCE_MAX) {
            sequenceGenerator.set(CmppConstants.SEQUENCE_START);
            return CmppConstants.SEQUENCE_START;
        }
        return seq;
    }
}
