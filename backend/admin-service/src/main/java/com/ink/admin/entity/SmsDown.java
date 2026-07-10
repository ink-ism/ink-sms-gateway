package com.ink.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 下行短信记录实体
 */
@Data
public class SmsDown {

    private Long id;
    private String msgId;
    private String serverMsgId;
    private String srcId;
    private String destTerminalId;
    private String msgContent;
    private Integer msgFmt;
    private String serviceId;
    private String channelCode;
    private Integer status;
    private String statusReport;
    private String errorMsg;
    private LocalDateTime createTime;
}
