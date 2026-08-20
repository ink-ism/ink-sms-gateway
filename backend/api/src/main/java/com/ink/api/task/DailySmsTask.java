package com.ink.api.task;

import com.ink.api.service.DownstreamPushService;
import com.ink.api.service.SmsRecordService;
import com.ink.common.utils.PhoneCarrierUtil;
import com.ink.common.utils.SignatureUtil;
import com.ink.core.connection.CmppConnectionManager;
import com.ink.core.connection.CmppConnectionManager.SubmitResult;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 每日定时短信发送任务
 * 每天 9:00 自动发送随机数量的测试短信到目标号码池
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(DailySmsTask.DailySmsConfig.class)
public class DailySmsTask {

    private final CmppConnectionManager connectionManager;
    private final SmsRecordService smsRecordService;
    private final DailySmsConfig config;
    private final Random random = new Random();

    /**
     * 每天 9:00 执行
     */
    @Scheduled(cron = "${ink.daily-sms.cron:0 0 9 * * ?}", zone = "Asia/Shanghai")
    public void execute() {
        if (!config.isEnabled()) {
            return;
        }
        log.info("每日定时短信任务开始执行");
        doSend();
    }

    /**
     * 执行发送逻辑（供定时任务和手动触发共用）
     * @return 发送结果统计
     */
    public SendResult doSend() {
        if (!connectionManager.isConnected()) {
            log.warn("CMPP 上游连接未就绪，跳过发送");
            return new SendResult(0, 0, 0);
        }

        int count = config.getMinCount() + random.nextInt(config.getMaxCount() - config.getMinCount() + 1);
        log.info("计划发送 {} 条短信（范围 {}-{}）", count, config.getMinCount(), config.getMaxCount());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < count; i++) {
            try {
                sendOne();
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
                log.error("发送失败 [{}/{}]: {}", i + 1, count, e.getMessage());
            }

            // 随机间隔 100-500ms，模拟真实流量
            try {
                Thread.sleep(100 + random.nextInt(400));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("每日定时短信任务完成: 成功={}, 失败={}, 总计={}", successCount.get(), failCount.get(), count);
        return new SendResult(count, successCount.get(), failCount.get());
    }

    private void sendOne() {
        // 随机选号码
        String destPhone = config.getDestPhones().get(random.nextInt(config.getDestPhones().size()));
        // 随机选模板
        String template = config.getTemplates().get(random.nextInt(config.getTemplates().size()));
        // 替换变量
        String content = template.replace("${random}", String.valueOf(100000 + random.nextInt(900000)));
        // 生成源号码：10690700 + 5位随机数
        String srcId = config.getSrcIdPrefix() + String.format("%05d", random.nextInt(100000));

        // 构建 Submit 请求
        com.ink.channel.cmpp.message.CmppSubmitRequestMessage submitReq =
                new com.ink.channel.cmpp.message.CmppSubmitRequestMessage();
        submitReq.setSrcId(srcId);
        submitReq.setDestTerminalId(new String[]{destPhone});
        submitReq.setMsgContent(content);
        submitReq.setMsgFmt(8); // UCS2
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String clientMsgId = config.getSpId() + timestamp + uuidPart;
        submitReq.setMsgId(clientMsgId.hashCode());
        submitReq.setServiceId("0000000000");
        submitReq.setRegisteredDelivery(1); // 请求状态报告

        String spId = config.getSpId();
        // 提交发送（关联到真实客户）
        CompletableFuture<SubmitResult> future = connectionManager.submit(submitReq, spId);

        try {
            SubmitResult result = future.orTimeout(10, TimeUnit.SECONDS).get();
            String serverMsgIdHex = Long.toHexString(result.getServerMsgId());
            log.debug("每日短信发送成功: spId={}, dest={}, clientMsgId={}, serverMsgId=0x{}", spId, destPhone, clientMsgId, serverMsgIdHex);
            String signature = SignatureUtil.extract(content);
            String carrier = PhoneCarrierUtil.resolve(destPhone);
            smsRecordService.recordSmsDown(clientMsgId, serverMsgIdHex, spId, srcId, destPhone,
                    content, 8, "0000000000", result.getChannelCode(), 1, null, null, null, signature, carrier);
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("每日短信发送失败: spId={}, dest={}, clientMsgId={}, error={}", spId, destPhone, clientMsgId, errorMsg);
            String signature = SignatureUtil.extract(content);
            String carrier = PhoneCarrierUtil.resolve(destPhone);
            smsRecordService.recordSmsDown(clientMsgId, null, spId, srcId, destPhone,
                    content, 8, "0000000000", null, 2, errorMsg, null, null, signature, carrier);
            throw new RuntimeException(errorMsg, e);
        }
    }

    @Data
    public static class SendResult {
        private final int total;
        private final int success;
        private final int failed;

        public SendResult(int total, int success, int failed) {
            this.total = total;
            this.success = success;
            this.failed = failed;
        }
    }

    @Data
    @ConfigurationProperties(prefix = "ink.daily-sms")
    public static class DailySmsConfig {
        /** 是否启用 */
        private boolean enabled = true;
        /** Cron 表达式 */
        private String cron = "0 0 9 * * ?";
        /** 最小发送条数 */
        private int minCount = 5000;
        /** 最大发送条数 */
        private int maxCount = 20000;
        /** 关联客户标识（ink_sp.sp_id） */
        private String spId = "testsp01";
        /** 源号码前缀 */
        private String srcIdPrefix = "10690700";
        /** 目标号码池 */
        private List<String> destPhones = List.of("18545654350", "19223308450", "13164564350", "18846936654");
        /** 消息模板池（支持 ${random} 变量） */
        private List<String> templates = List.of(
                "【墨轩科技】今日天气晴好，注意防晒，退订回复R",
                "【墨轩科技】您的验证码是${random}，5分钟内有效，请勿泄露"
        );
    }
}
