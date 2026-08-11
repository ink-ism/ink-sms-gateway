package com.ink.api.controller;

import com.ink.common.utils.Result;
import com.ink.core.connection.CmppConnectionManager;
import com.ink.core.connection.CmppConnectionManager.SubmitResult;
import com.ink.api.service.DownstreamPushService;
import com.ink.api.service.RateLimiterService;
import com.ink.api.service.SensitiveWordService;
import com.ink.api.service.SignatureTemplateService;
import com.ink.api.service.SmsRecordService;
import com.ink.api.session.SpSessionManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 短信 REST API 控制器
 * 供前端调用，提供短信发送和状态查询能力
 * 使用异步 Servlet 模式，不阻塞 Tomcat 线程
 * REST 入口（平台直发）不计费，但受 IP 限流与敏感词过滤约束
 */
@Slf4j
@RestController
@RequestMapping("/api/sms")
public class SmsController {

    private final CmppConnectionManager connectionManager;
    private final SpSessionManager sessionManager;
    private final SmsRecordService smsRecordService;
    private final RateLimiterService rateLimiterService;
    private final SensitiveWordService sensitiveWordService;
    private final SignatureTemplateService signatureTemplateService;

    public SmsController(CmppConnectionManager connectionManager, SpSessionManager sessionManager,
                          SmsRecordService smsRecordService, RateLimiterService rateLimiterService,
                          SensitiveWordService sensitiveWordService,
                          SignatureTemplateService signatureTemplateService) {
        this.connectionManager = connectionManager;
        this.sessionManager = sessionManager;
        this.smsRecordService = smsRecordService;
        this.rateLimiterService = rateLimiterService;
        this.sensitiveWordService = sensitiveWordService;
        this.signatureTemplateService = signatureTemplateService;
    }

    /**
     * 发送短信（异步，不阻塞 Tomcat 线程）
     * 前置校验：IP 限流 → 签名/模板校验 → 敏感词过滤
     */
    @PostMapping("/send")
    public CompletableFuture<Result<SmsSendResponse>> sendSms(@RequestBody SmsSendRequest request,
                                                              HttpServletRequest httpRequest) {
        log.info("收到短信发送请求: phone={}, content={}", request.getPhone(), request.getContent());

        // 1. IP 限流
        String clientIp = resolveClientIp(httpRequest);
        if (!rateLimiterService.tryAcquireIp(clientIp)) {
            log.warn("REST 发送被 IP 限速拦截: ip={}, phone={}", clientIp, request.getPhone());
            return CompletableFuture.completedFuture(Result.error("发送频率超限，请稍后重试"));
        }

        // 2. 签名/模板校验与内容组装
        String content = request.getContent();
        if (request.getTemplateId() != null) {
            String templateContent = signatureTemplateService.findApprovedTemplate(request.getTemplateId());
            if (templateContent == null) {
                return CompletableFuture.completedFuture(Result.error("模板不存在或未通过审核"));
            }
            String signature = null;
            if (request.getSignatureId() != null) {
                signature = signatureTemplateService.findApprovedSignature(request.getSignatureId());
                if (signature == null) {
                    return CompletableFuture.completedFuture(Result.error("签名不存在或未通过审核"));
                }
            }
            content = signatureTemplateService.assembleContent(templateContent, signature);
        } else if (request.getSignatureId() != null) {
            String signature = signatureTemplateService.findApprovedSignature(request.getSignatureId());
            if (signature == null) {
                return CompletableFuture.completedFuture(Result.error("签名不存在或未通过审核"));
            }
            content = signatureTemplateService.assembleContent(content != null ? content : "", signature);
        }

        // 3. 敏感词过滤
        String hitWord = sensitiveWordService.match(content);
        if (hitWord != null) {
            log.warn("REST 发送命中敏感词: ip={}, phone={}, word={}", clientIp, request.getPhone(), hitWord);
            String rejectMsgId = "SMS" + System.currentTimeMillis()
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            smsRecordService.recordSmsDown(rejectMsgId, null, DownstreamPushService.REST_SP_ID,
                    request.getSrcId(), request.getPhone(), content,
                    request.getMsgFmt() != null ? request.getMsgFmt() : 8, request.getServiceId(),
                    null, 2, "内容包含敏感词", null, null);
            return CompletableFuture.completedFuture(Result.error("内容包含敏感词"));
        }

        if (!connectionManager.isConnected()) {
            log.warn("CMPP 上游连接未就绪");
            return CompletableFuture.completedFuture(Result.error("CMPP 上游连接未就绪"));
        }

        // 构建 Submit 请求
        com.ink.channel.cmpp.message.CmppSubmitRequestMessage submitReq =
                new com.ink.channel.cmpp.message.CmppSubmitRequestMessage();
        submitReq.setSrcId(request.getSrcId() != null ? request.getSrcId() : "10690000");
        submitReq.setDestTerminalId(new String[]{request.getPhone()});
        submitReq.setMsgContent(content);
        submitReq.setMsgFmt(request.getMsgFmt() != null ? request.getMsgFmt() : 8); // 默认 UCS2
        // 生成唯一客户端 msgId：SMS + 时间戳 + 8位UUID
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String clientMsgId = "SMS" + timestamp + uuidPart;
        submitReq.setMsgId(clientMsgId.hashCode()); // CMPP 协议需要 int 类型的 msgId
        submitReq.setServiceId(request.getServiceId() != null ? request.getServiceId() : "0000000000");

        String destPhone = request.getPhone();
        String finalContent = content;
        // REST 入口客户标识固定为 REST（仅落库，不推送下游）
        CompletableFuture<SubmitResult> submitFuture =
                connectionManager.submit(submitReq, DownstreamPushService.REST_SP_ID);

        // 异步等待 CMPP Submit Response，释放 Tomcat 线程
        return submitFuture
                .orTimeout(10, TimeUnit.SECONDS)
                .handle((result, ex) -> {
                    SmsSendResponse response = new SmsSendResponse();
                    if (ex != null) {
                        Throwable cause = ex instanceof java.util.concurrent.CompletionException && ex.getCause() != null
                                ? ex.getCause() : ex;
                        String causeMsg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
                        boolean blacklisted = causeMsg.startsWith("BLACKLISTED:");
                        String errorMsg = ex instanceof TimeoutException
                                ? "CMPP Submit 响应超时(10s)"
                                : (blacklisted ? "号码已退订" : causeMsg);
                        log.error("短信提交失败: clientMsgId={}, error={}", clientMsgId, errorMsg);
                        smsRecordService.recordSmsDown(clientMsgId, null, DownstreamPushService.REST_SP_ID,
                                submitReq.getSrcId(), destPhone,
                                finalContent, submitReq.getMsgFmt(), submitReq.getServiceId(),
                                null, 2, errorMsg, null, null);
                        response.setSuccess(false);
                        response.setMessage(errorMsg);
                        return Result.<SmsSendResponse>error(errorMsg);
                    } else {
                        String serverMsgIdHex = Long.toHexString(result.getServerMsgId());
                        String channelCode = result.getChannelCode();
                        log.info("短信提交成功: clientMsgId={}, serverMsgId=0x{}, channel={}",
                                clientMsgId, serverMsgIdHex, channelCode);
                        smsRecordService.recordSmsDown(clientMsgId, serverMsgIdHex, DownstreamPushService.REST_SP_ID,
                                submitReq.getSrcId(), destPhone,
                                finalContent, submitReq.getMsgFmt(), submitReq.getServiceId(),
                                channelCode, 1, null, null, null);
                        response.setSuccess(true);
                        response.setMessage("短信已提交");
                        return Result.success(response);
                    }
                });
    }

    /**
     * 查询连接状态
     */
    @GetMapping("/status")
    public Result<StatusResponse> getStatus() {
        StatusResponse status = new StatusResponse();
        status.setUpstreamConnected(connectionManager.isConnected());
        status.setActiveSpCount(sessionManager.getActiveCount());
        status.setPoolSize(connectionManager.getPoolSize());
        status.setConnectedCount(connectionManager.getConnectedCount());
        return Result.success(status);
    }

    /**
     * 解析客户端真实 IP（支持反向代理 X-Forwarded-For）
     */
    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    @Data
    public static class SmsSendRequest {
        /** 手机号 */
        private String phone;
        /** 短信内容（指定 templateId 时以模板内容为准） */
        private String content;
        /** 源号码 */
        private String srcId;
        /** 业务标识 */
        private String serviceId;
        /** 消息格式：0=ASCII, 8=UCS2, 15=GB2312 */
        private Integer msgFmt;
        /** 签名ID（可选，需已通过审核） */
        private Long signatureId;
        /** 模板ID（可选，需已通过审核） */
        private Long templateId;
    }

    @Data
    public static class SmsSendResponse {
        private boolean success;
        private String message;
    }

    @Data
    public static class StatusResponse {
        private boolean upstreamConnected;
        private int activeSpCount;
        private int poolSize;
        private int connectedCount;
    }
}
