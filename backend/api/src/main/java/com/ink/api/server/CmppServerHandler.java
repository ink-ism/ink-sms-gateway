package com.ink.api.server;

import com.ink.api.config.CmppServerConfig;
import com.ink.api.session.SpSessionManager;
import com.ink.channel.cmpp.CmppCommandType;
import com.ink.channel.cmpp.CmppConstants;
import com.ink.channel.cmpp.CmppMessage;
import com.ink.channel.cmpp.message.CmppActiveTestResponseMessage;
import com.ink.channel.cmpp.message.CmppConnectRequestMessage;
import com.ink.channel.cmpp.message.CmppConnectResponseMessage;
import com.ink.channel.cmpp.message.CmppSubmitRequestMessage;
import com.ink.channel.cmpp.message.CmppSubmitResponseMessage;
import com.ink.channel.cmpp.message.CmppTerminateResponseMessage;
import com.ink.channel.cmpp.util.CmppAuthUtil;
import com.ink.channel.session.CmppSession;
import com.ink.core.connection.CmppConnectionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * CMPP 服务端消息处理器
 * 处理下游 SP 的 Connect 认证、Submit 请求、ActiveTest 心跳、Terminate 退出
 */
@Slf4j
public class CmppServerHandler extends ChannelInboundHandlerAdapter {

    private final CmppServerConfig serverConfig;
    private final SpSessionManager sessionManager;
    private CmppSession session;

    /** 上游连接管理器（通过构造函数注入，可为 null） */
    private static CmppConnectionManager connectionManager;

    public static void setConnectionManager(CmppConnectionManager manager) {
        connectionManager = manager;
    }

    public CmppServerHandler(CmppServerConfig serverConfig, SpSessionManager sessionManager) {
        this.serverConfig = serverConfig;
        this.sessionManager = sessionManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String remoteAddr = ctx.channel().remoteAddress().toString();
        log.info("SP 连接接入: {}", remoteAddr);
        // 创建临时会话，认证后替换 spId
        session = new CmppSession(ctx.channel(), "unknown");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof CmppMessage cmppMsg)) {
            return;
        }

        int commandId = cmppMsg.getCommandId();
        int sequenceId = cmppMsg.getSequenceId();
        byte[] body = cmppMsg.getBody();

        CmppCommandType cmdType = CmppCommandType.fromCommandId(commandId);
        if (cmdType == null) {
            log.warn("未知命令类型: 0x{}", Integer.toHexString(commandId));
            return;
        }

        session.updateActiveTime();

        switch (cmdType) {
            case CONNECT -> handleConnect(sequenceId, body);
            case SUBMIT -> handleSubmit(sequenceId, body);
            case ACTIVE_TEST -> handleActiveTest(sequenceId);
            case TERMINATE -> handleTerminate(sequenceId);
            default -> log.warn("不支持的命令: {}", cmdType.getDescription());
        }
    }

    /**
     * 处理 Connect 认证请求
     */
    private void handleConnect(int sequenceId, byte[] body) {
        CmppConnectRequestMessage connectReq = CmppConnectRequestMessage.fromBytes(body);
        String spId = connectReq.getSourceAddr();
        log.info("收到 SP Connect 请求: spId={}", spId);

        CmppConnectResponseMessage connectResp = new CmppConnectResponseMessage();
        connectResp.setVersion(CmppConstants.VERSION);

        // 验证 SP 合法性
        String secret = findSpSecret(spId);
        if (secret == null) {
            log.warn("非法 SP: {}", spId);
            connectResp.setStatus(2); // 非法源地址
            sendResponse(CmppCommandType.CONNECT_RESP.getCommandId(), sequenceId, connectResp.toBytes());
            return;
        }

        // 验证认证密码
        byte[] expectedAuth = CmppAuthUtil.generateAuthenticatorServer(spId, secret, connectReq.getTimestamp());
        if (!CmppAuthUtil.verifyAuthenticator(expectedAuth, connectReq.getAuthenticatorClient())) {
            log.warn("SP 认证失败: {}", spId);
            connectResp.setStatus(3); // 认证错
            sendResponse(CmppCommandType.CONNECT_RESP.getCommandId(), sequenceId, connectResp.toBytes());
            return;
        }

        // 认证成功
        connectResp.setStatus(0);
        connectResp.setAuthenticatorIsmg(CmppAuthUtil.generateAuthenticatorServer(spId, secret, connectReq.getTimestamp()));
        sendResponse(CmppCommandType.CONNECT_RESP.getCommandId(), sequenceId, connectResp.toBytes());

        // 更新会话
        session.setSpId(spId);
        session.markAuthenticated();
        sessionManager.addSession(spId, session);
        log.info("SP 认证成功: spId={}", spId);
    }

    /**
     * 处理 Submit 短信提交请求
     */
    private void handleSubmit(int sequenceId, byte[] body) {
        if (!session.isAuthenticated()) {
            log.warn("SP 未认证，拒绝 Submit");
            CmppSubmitResponseMessage resp = new CmppSubmitResponseMessage();
            resp.setResult(3);
            sendResponse(CmppCommandType.SUBMIT_RESP.getCommandId(), sequenceId, resp.toBytes());
            return;
        }

        CmppSubmitRequestMessage submitReq = CmppSubmitRequestMessage.fromBytes(body);
        log.info("收到 SP Submit: spId={}, dest={}", session.getSpId(), submitReq.getDestTerminalId());

        // 转发到上游 CMPP 服务器，异步等待 Submit Response
        if (connectionManager != null && connectionManager.isConnected()) {
            connectionManager.submit(submitReq).whenComplete((result, ex) -> {
                CmppSubmitResponseMessage submitResp = new CmppSubmitResponseMessage();
                if (ex == null) {
                    submitResp.setMsgId(result.getServerMsgId());
                    submitResp.setResult(0);
                    log.info("SP Submit 转发成功: serverMsgId=0x{}", Long.toHexString(result.getServerMsgId()));
                } else {
                    submitResp.setMsgId(System.currentTimeMillis());
                    submitResp.setResult(2);
                    log.warn("SP Submit 转发失败: {}", ex.getMessage());
                }
                sendResponse(CmppCommandType.SUBMIT_RESP.getCommandId(), sequenceId, submitResp.toBytes());
            });
        } else {
            log.warn("上游 CMPP 未连接，无法转发 Submit");
            CmppSubmitResponseMessage submitResp = new CmppSubmitResponseMessage();
            submitResp.setResult(1);
            sendResponse(CmppCommandType.SUBMIT_RESP.getCommandId(), sequenceId, submitResp.toBytes());
        }
    }

    /**
     * 处理 ActiveTest 心跳
     */
    private void handleActiveTest(int sequenceId) {
        log.debug("收到 SP ActiveTest: spId={}", session.getSpId());
        CmppActiveTestResponseMessage resp = new CmppActiveTestResponseMessage();
        resp.setReserved(0);
        resp.setErrorCode(0);
        sendResponse(CmppCommandType.ACTIVE_TEST_RESP.getCommandId(), sequenceId, resp.toBytes());
    }

    /**
     * 处理 Terminate 退出
     */
    private void handleTerminate(int sequenceId) {
        log.info("收到 SP Terminate: spId={}", session.getSpId());
        CmppTerminateResponseMessage resp = new CmppTerminateResponseMessage();
        sendResponse(CmppCommandType.TERMINATE_RESP.getCommandId(), sequenceId, resp.toBytes());

        sessionManager.removeSession(session.getSpId());
        session.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            log.warn("SP 连接空闲超时，关闭: {}", session.getSpId());
            sessionManager.removeSession(session.getSpId());
            ctx.close();
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("SP 连接断开: {}", session.getSpId());
        sessionManager.removeSession(session.getSpId());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("SP 连接异常: {}", cause.getMessage(), cause);
        sessionManager.removeSession(session.getSpId());
        ctx.close();
    }

    /**
     * 发送响应消息
     */
    private void sendResponse(int commandId, int sequenceId, byte[] respBody) {
        CmppMessage resp = CmppMessage.create(commandId, sequenceId, respBody);
        session.send(resp);
    }

    /**
     * 查找 SP 的共享密钥
     */
    private String findSpSecret(String spId) {
        String allowedSpList = serverConfig.getAllowedSpList();
        if (allowedSpList == null || allowedSpList.isEmpty()) {
            return null;
        }
        for (String entry : allowedSpList.split(",")) {
            String[] parts = entry.trim().split(":");
            if (parts.length == 2 && parts[0].equals(spId)) {
                return parts[1];
            }
        }
        return null;
    }
}
