package com.ink.api.controller;

import com.ink.common.utils.Result;
import com.ink.core.connection.CmppConnectionManager;
import com.ink.core.connection.CmppConnectionManager.SubmitResult;
import com.ink.api.service.DownstreamPushService;
import com.ink.api.service.SmsRecordService;
import com.ink.api.session.SpSessionManager;
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
 */
@Slf4j
@RestController
@RequestMapping("/api/sms")
public class SmsController {

    private final CmppConnectionManager connectionManager;
    private final SpSessionManager sessionManager;
    private final SmsRecordService smsRecordService;

    public SmsController(CmppConnectionManager connectionManager, SpSessionManager sessionManager,
                          SmsRecordService smsRecordService) {
        this.connectionManager = connectionManager;
        this.sessionManager = sessionManager;
        this.smsRecordService = smsRecordService;
    }

    /**
     * 发送短信（异步，不阻塞 Tomcat 线程）
     */
    @PostMapping("/send")
    public CompletableFuture<Result<SmsSendResponse>> sendSms(@RequestBody SmsSendRequest request) {
        log.info("收到短信发送请求: phone={}, content={}", request.getPhone(), request.getContent());

        if (!connectionManager.isConnected()) {
            log.warn("CMPP 上游连接未就绪");
            return CompletableFuture.completedFuture(Result.error("CMPP 上游连接未就绪"));
        }

        // 构建 Submit 请求
        com.ink.channel.cmpp.message.CmppSubmitRequestMessage submitReq =
                new com.ink.channel.cmpp.message.CmppSubmitRequestMessage();
        submitReq.setSrcId(request.getSrcId() != null ? request.getSrcId() : "10690000");
        submitReq.setDestTerminalId(new String[]{request.getPhone()});
        submitReq.setMsgContent(request.getContent());
        submitReq.setMsgFmt(request.getMsgFmt() != null ? request.getMsgFmt() : 8); // 默认 UCS2
        // 生成唯一客户端 msgId：SMS + 时间戳 + 8位UUID
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String clientMsgId = "SMS" + timestamp + uuidPart;
        submitReq.setMsgId(clientMsgId.hashCode()); // CMPP 协议需要 int 类型的 msgId
        submitReq.setServiceId(request.getServiceId() != null ? request.getServiceId() : "0000000000");

        String destPhone = request.getPhone();
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
                                request.getContent(), submitReq.getMsgFmt(), submitReq.getServiceId(),
                                null, 2, errorMsg);
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
                                request.getContent(), submitReq.getMsgFmt(), submitReq.getServiceId(),
                                channelCode, 1, null);
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

    @Data
    public static class SmsSendRequest {
        /** 手机号 */
        private String phone;
        /** 短信内容 */
        private String content;
        /** 源号码 */
        private String srcId;
        /** 业务标识 */
        private String serviceId;
        /** 消息格式：0=ASCII, 8=UCS2, 15=GB2312 */
        private Integer msgFmt;
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
