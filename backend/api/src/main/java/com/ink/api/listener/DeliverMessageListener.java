package com.ink.api.listener;

import com.ink.channel.cmpp.CmppCommandType;
import com.ink.channel.cmpp.CmppMessage;
import com.ink.channel.cmpp.message.CmppDeliverRequestMessage;
import com.ink.api.service.SmsRecordService;
import com.ink.core.connection.CmppConnectionManager;
import com.ink.core.handler.CmppMessageHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Deliver 消息监听器
 * 监听上游 CMPP 服务器推送的 Deliver 消息（上行短信、状态报告），写入数据库
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliverMessageListener {

    private final CmppConnectionManager connectionManager;
    private final SmsRecordService smsRecordService;

    @PostConstruct
    public void init() {
        connectionManager.setMessageHandler(new CmppMessageHandler() {
            @Override
            public void handleMessage(CmppMessage message) {
                try {
                    int commandId = message.getCommandId();
                    CmppCommandType cmdType = CmppCommandType.fromCommandId(commandId);
                    if (cmdType == CmppCommandType.DELIVER) {
                        CmppDeliverRequestMessage deliver = CmppDeliverRequestMessage.fromBytes(message.getBody());
                        String msgIdStr = Long.toHexString(deliver.getMsgId());

                        if (deliver.isReport()) {
                            // 状态报告不记录上行短信，而是更新下行记录的状态
                            // 使用报告中的 reportMsgId（原始 Submit 的 Msg_Id）查找下行记录
                            // 同时传入 srcTerminalId（对应原始请求的 dest_terminal_id）精确定位记录
                            String reportMsgIdHex = Long.toHexString(deliver.getReportMsgId());
                            String destTerminalId = deliver.getSrcTerminalId();
                            log.info("收到状态报告: msgId=0x{}, reportMsgId=0x{}, stat={}, srcTerminal={}",
                                    msgIdStr, reportMsgIdHex, deliver.getReportStat(), destTerminalId);
                            smsRecordService.updateSmsDownStatus(reportMsgIdHex, destTerminalId, deliver.getReportStat());
                        } else {
                            // 普通上行短信记录到上行表
                            log.info("收到上行短信: msgId=0x{}, src={}, dest={}, content={}",
                                    msgIdStr, deliver.getSrcTerminalId(), deliver.getDestId(), deliver.getMsgContent());
                            smsRecordService.recordSmsUp(msgIdStr, deliver.getSrcTerminalId(),
                                    deliver.getDestId(), deliver.getMsgContent(), deliver.getMsgFmt(),
                                    deliver.getServiceId(), null);
                        }
                    }
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
}
