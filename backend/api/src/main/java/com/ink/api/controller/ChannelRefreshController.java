package com.ink.api.controller;

import com.ink.common.utils.Result;
import com.ink.core.connection.CmppConnectionManager;
import com.ink.core.connection.CmppConnectionManager.SubmitResult;
import com.ink.api.service.DownstreamPushService;
import com.ink.api.service.SmsRecordService;
import com.ink.api.service.SpAccountService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 通道管理控制器
 * 提供通道热加载、状态查询和测试发送接口
 */
@Slf4j
@RestController
@RequestMapping("/api/sms/channels")
@RequiredArgsConstructor
public class ChannelRefreshController {

    private final CmppConnectionManager connectionManager;
    private final SmsRecordService smsRecordService;
    private final SpAccountService spAccountService;

    /**
     * 刷新配置（从数据库重新加载通道与下游客户）
     */
    @PostMapping("/refresh")
    public Result<String> refreshChannels() {
        connectionManager.refreshChannels();
        spAccountService.reload();
        return Result.success("通道与客户刷新完成");
    }

    /**
     * 查询各通道连接状态
     */
    @GetMapping("/status")
    public Result<Map<String, Map<String, Object>>> getChannelStatus() {
        return Result.success(connectionManager.getChannelStatus());
    }

    /**
     * 通道测试发送
     * 向指定通道发送一条测试短信，验证通道连通性
     */
    @PostMapping("/test")
    public CompletableFuture<Result<TestSendResponse>> testSend(@RequestBody TestSendRequest request) {
        String channelCode = request.getChannelCode();
        log.info("通道测试发送: channel={}, phone={}, content={}", channelCode, request.getPhone(), request.getContent());

        if (!connectionManager.isChannelConnected(channelCode)) {
            return CompletableFuture.completedFuture(Result.error("通道 [" + channelCode + "] 未连接"));
        }

        // 构建 Submit 请求
        com.ink.channel.cmpp.message.CmppSubmitRequestMessage submitReq =
                new com.ink.channel.cmpp.message.CmppSubmitRequestMessage();
        submitReq.setSrcId(request.getSrcId() != null ? request.getSrcId() : "10690000");
        submitReq.setDestTerminalId(new String[]{request.getPhone()});
        submitReq.setMsgContent(request.getContent());
        submitReq.setMsgFmt(request.getMsgFmt() != null ? request.getMsgFmt() : 8);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String clientMsgId = "TST" + timestamp + uuidPart;
        submitReq.setMsgId(clientMsgId.hashCode());
        submitReq.setServiceId(request.getServiceId() != null ? request.getServiceId() : "0000000000");

        String destPhone = request.getPhone();
        CompletableFuture<SubmitResult> submitFuture = connectionManager.submitToChannel(channelCode, submitReq);

        return submitFuture
                .orTimeout(10, TimeUnit.SECONDS)
                .handle((result, ex) -> {
                    TestSendResponse response = new TestSendResponse();
                    if (ex != null) {
                        Throwable cause = ex instanceof java.util.concurrent.CompletionException && ex.getCause() != null
                                ? ex.getCause() : ex;
                        String causeMsg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
                        boolean blacklisted = causeMsg.startsWith("BLACKLISTED:");
                        String errorMsg = ex instanceof TimeoutException
                                ? "CMPP Submit 响应超时(10s)"
                                : (blacklisted ? "号码已退订" : causeMsg);
                        log.error("测试发送失败: channel={}, clientMsgId={}, error={}", channelCode, clientMsgId, errorMsg);
                        smsRecordService.recordSmsDown(clientMsgId, null, DownstreamPushService.REST_SP_ID,
                                submitReq.getSrcId(), destPhone,
                                request.getContent(), submitReq.getMsgFmt(), submitReq.getServiceId(),
                                channelCode, 2, errorMsg);
                        response.setSuccess(false);
                        response.setMessage("发送失败: " + errorMsg);
                        return Result.<TestSendResponse>error(response.getMessage());
                    } else {
                        String serverMsgIdHex = Long.toHexString(result.getServerMsgId());
                        log.info("测试发送成功: channel={}, clientMsgId={}, serverMsgId=0x{}",
                                channelCode, clientMsgId, serverMsgIdHex);
                        smsRecordService.recordSmsDown(clientMsgId, serverMsgIdHex, DownstreamPushService.REST_SP_ID,
                                submitReq.getSrcId(), destPhone,
                                request.getContent(), submitReq.getMsgFmt(), submitReq.getServiceId(),
                                channelCode, 1, null);
                        response.setSuccess(true);
                        response.setMessage("发送成功");
                        response.setServerMsgId(serverMsgIdHex);
                        return Result.success(response);
                    }
                });
    }

    @Data
    public static class TestSendRequest {
        private String channelCode;
        private String phone;
        private String content;
        private String srcId;
        private String serviceId;
        private Integer msgFmt;
    }

    @Data
    public static class TestSendResponse {
        private boolean success;
        private String message;
        private String serverMsgId;
    }
}
