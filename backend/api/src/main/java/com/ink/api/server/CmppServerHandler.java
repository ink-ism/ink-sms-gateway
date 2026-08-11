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
import com.ink.api.service.BillingService;
import com.ink.api.service.DownstreamPushService;
import com.ink.api.service.RateLimiterService;
import com.ink.api.service.SensitiveWordService;
import com.ink.api.service.SpAccountService;
import com.ink.api.service.SmsRecordService;
import com.ink.core.connection.CmppConnectionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletionException;

/**
 * CMPP 服务端消息处理器
 * 处理下游 SP 的 Connect 认证、Submit 请求、ActiveTest 心跳、Terminate 退出
 */
@Slf4j
public class CmppServerHandler extends ChannelInboundHandlerAdapter {

    private final CmppServerConfig serverConfig;
    private final SpSessionManager sessionManager;
    private CmppSession session;

    /** 上游连接管理器（通过静态注入，可为 null） */
    private static CmppConnectionManager connectionManager;

    /** 短信记录服务（通过静态注入，可为 null） */
    private static SmsRecordService smsRecordService;

    /** 客户账号服务（通过静态注入，可为 null） */
    private static SpAccountService spAccountService;

    /** 下游推送服务（通过静态注入，可为 null） */
    private static DownstreamPushService pushService;

    /** 计费服务（通过静态注入，可为 null） */
    private static BillingService billingService;

    /** 限流服务（通过静态注入，可为 null） */
    private static RateLimiterService rateLimiterService;

    /** 敏感词服务（通过静态注入，可为 null） */
    private static SensitiveWordService sensitiveWordService;

    /** Submit 响应结果码：号码已退订（黑名单拦截，自定义码） */
    private static final int SUBMIT_RESULT_BLACKLISTED = 9;

    /** Submit 响应结果码：内容命中敏感词（自定义码） */
    private static final int SUBMIT_RESULT_SENSITIVE = 8;

    /** Submit 响应结果码：余额不足（自定义码） */
    private static final int SUBMIT_RESULT_INSUFFICIENT_BALANCE = 6;

    /** Submit 响应结果码：发送限速（自定义码） */
    private static final int SUBMIT_RESULT_RATE_LIMITED = 10;

    public static void setConnectionManager(CmppConnectionManager manager) {
        connectionManager = manager;
    }

    public static void setSmsRecordService(SmsRecordService service) {
        smsRecordService = service;
    }

    public static void setSpAccountService(SpAccountService service) {
        spAccountService = service;
    }

    public static void setPushService(DownstreamPushService service) {
        pushService = service;
    }

    public static void setBillingService(BillingService service) {
        billingService = service;
    }

    public static void setRateLimiterService(RateLimiterService service) {
        rateLimiterService = service;
    }

    public static void setSensitiveWordService(SensitiveWordService service) {
        sensitiveWordService = service;
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

        // 认证成功后异步补发离线期间的回执/上行消息
        if (pushService != null) {
            pushService.flushQueueAsync(spId);
        }
    }

    /**
     * 处理 Submit 短信提交请求
     * 拦截顺序：限流 → 敏感词 → 上游连接检查 → 余额预扣 → 转发（失败返还）
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
        String spId = session.getSpId();
        String destPhone = (submitReq.getDestTerminalId() != null && submitReq.getDestTerminalId().length > 0)
                ? submitReq.getDestTerminalId()[0] : null;
        log.info("收到 SP Submit: spId={}, dest={}", spId, destPhone);

        // 生成唯一客户端 msgId
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String clientMsgId = "CMPP" + timestamp + uuidPart;

        // 1. 限流拦截
        if (rateLimiterService != null && !rateLimiterService.tryAcquireSp(spId)) {
            log.warn("SP Submit 被限速拦截: spId={}, dest={}", spId, destPhone);
            rejectSubmit(sequenceId, clientMsgId, spId, submitReq, destPhone,
                    SUBMIT_RESULT_RATE_LIMITED, "发送频率超限");
            return;
        }

        // 2. 敏感词拦截
        if (sensitiveWordService != null) {
            String hitWord = sensitiveWordService.match(submitReq.getMsgContent());
            if (hitWord != null) {
                log.warn("SP Submit 命中敏感词: spId={}, dest={}, word={}", spId, destPhone, hitWord);
                rejectSubmit(sequenceId, clientMsgId, spId, submitReq, destPhone,
                        SUBMIT_RESULT_SENSITIVE, "内容包含敏感词");
                return;
            }
        }

        // 3. 上游连接检查
        if (connectionManager == null || !connectionManager.isConnected()) {
            log.warn("上游 CMPP 未连接，无法转发 Submit");
            rejectSubmit(sequenceId, clientMsgId, spId, submitReq, destPhone, 1, "上游CMPP未连接");
            return;
        }

        // 4. 余额预扣（返回 null 表示余额不足）
        BigDecimal fee = billingService != null ? billingService.tryDeduct(spId, clientMsgId) : BigDecimal.ZERO;
        if (fee == null) {
            log.warn("SP Submit 余额不足: spId={}, dest={}", spId, destPhone);
            rejectSubmit(sequenceId, clientMsgId, spId, submitReq, destPhone,
                    SUBMIT_RESULT_INSUFFICIENT_BALANCE, "余额不足");
            return;
        }

        // 5. 转发到上游 CMPP 服务器（携带客户标识：绑定通道选路 + 黑名单拦截）
        connectionManager.submit(submitReq, spId).whenComplete((result, ex) -> {
            CmppSubmitResponseMessage submitResp = new CmppSubmitResponseMessage();
            if (ex == null) {
                submitResp.setMsgId(result.getServerMsgId());
                submitResp.setResult(0);
                String serverMsgIdHex = Long.toHexString(result.getServerMsgId());
                String channelCode = result.getChannelCode();
                log.info("SP Submit 转发成功: serverMsgId=0x{}, channel={}", serverMsgIdHex, channelCode);
                // 记录下行短信（含扣费与通道成本）
                if (smsRecordService != null) {
                    BigDecimal cost = billingService != null ? billingService.findCostPrice(channelCode) : null;
                    smsRecordService.recordSmsDown(clientMsgId, serverMsgIdHex, spId, submitReq.getSrcId(),
                            destPhone, submitReq.getMsgContent(), submitReq.getMsgFmt(),
                            submitReq.getServiceId(), channelCode, 1, null, fee, cost);
                }
            } else if (isBlacklisted(ex)) {
                // 黑名单拦截：返还预扣余额，返回自定义结果码
                refundFee(spId, fee, clientMsgId);
                submitResp.setMsgId(0);
                submitResp.setResult(SUBMIT_RESULT_BLACKLISTED);
                log.warn("SP Submit 被黑名单拦截: spId={}, dest={}", spId, destPhone);
                if (smsRecordService != null) {
                    smsRecordService.recordSmsDown(clientMsgId, null, spId, submitReq.getSrcId(),
                            destPhone, submitReq.getMsgContent(), submitReq.getMsgFmt(),
                            submitReq.getServiceId(), null, 2, "号码已退订", null, null);
                }
            } else {
                // 转发失败：返还预扣余额
                refundFee(spId, fee, clientMsgId);
                submitResp.setMsgId(0);
                submitResp.setResult(2);
                log.warn("SP Submit 转发失败: {}", ex.getMessage());
                if (smsRecordService != null) {
                    smsRecordService.recordSmsDown(clientMsgId, null, spId, submitReq.getSrcId(),
                            destPhone, submitReq.getMsgContent(), submitReq.getMsgFmt(),
                            submitReq.getServiceId(), null, 2, ex.getMessage(), null, null);
                }
            }
            sendResponse(CmppCommandType.SUBMIT_RESP.getCommandId(), sequenceId, submitResp.toBytes());
        });
    }

    /**
     * 拒绝 Submit：返回指定结果码并落失败记录（未扣费，fee/cost 为 null）
     */
    private void rejectSubmit(int sequenceId, String clientMsgId, String spId,
                              CmppSubmitRequestMessage submitReq, String destPhone,
                              int resultCode, String errorMsg) {
        if (smsRecordService != null) {
            smsRecordService.recordSmsDown(clientMsgId, null, spId, submitReq.getSrcId(),
                    destPhone, submitReq.getMsgContent(), submitReq.getMsgFmt(),
                    submitReq.getServiceId(), null, 2, errorMsg, null, null);
        }
        CmppSubmitResponseMessage submitResp = new CmppSubmitResponseMessage();
        submitResp.setMsgId(0);
        submitResp.setResult(resultCode);
        sendResponse(CmppCommandType.SUBMIT_RESP.getCommandId(), sequenceId, submitResp.toBytes());
    }

    /**
     * 返还预扣余额（金额为 0 或计费服务不可用时跳过）
     */
    private void refundFee(String spId, BigDecimal fee, String clientMsgId) {
        if (billingService != null) {
            billingService.refund(spId, fee, clientMsgId);
        }
    }

    /**
     * 判断异常是否为黑名单拦截（解包 CompletionException）
     */
    private boolean isBlacklisted(Throwable ex) {
        Throwable cause = ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
        return cause.getMessage() != null
                && cause.getMessage().startsWith(CmppConnectionManager.BLACKLISTED_ERROR);
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

        sessionManager.removeSession(session.getSpId(), session);
        session.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            log.warn("SP 连接空闲超时，关闭: {}", session.getSpId());
            sessionManager.removeSession(session.getSpId(), session);
            ctx.close();
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("SP 连接断开: {}", session.getSpId());
        sessionManager.removeSession(session.getSpId(), session);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("SP 连接异常: {}", cause.getMessage(), cause);
        sessionManager.removeSession(session.getSpId(), session);
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
     * 优先查数据库客户表（ink_sp），未命中时回退到 yml 配置并提示迁移
     */
    private String findSpSecret(String spId) {
        if (spAccountService != null) {
            String secret = spAccountService.findSecret(spId);
            if (secret != null) {
                return secret;
            }
        }
        // 兜底：旧配置 allowed-sp-list（待迁移到客户表）
        String allowedSpList = serverConfig.getAllowedSpList();
        if (allowedSpList == null || allowedSpList.isEmpty()) {
            return null;
        }
        for (String entry : allowedSpList.split(",")) {
            String[] parts = entry.trim().split(":");
            if (parts.length == 2 && parts[0].equals(spId)) {
                log.warn("SP [{}] 使用 yml 兜底配置认证，请迁移到客户表 ink_sp", spId);
                return parts[1];
            }
        }
        return null;
    }
}
