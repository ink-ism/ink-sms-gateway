package com.ink.admin.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 下行短信详情 DTO
 */
@Data
public class SmsDownDetail {

    // ===== 下行短信信息 =====
    private Long id;
    private String msgId;
    private String serverMsgId;
    private String spId;
    private String srcId;
    private String destTerminalId;
    private String msgContent;
    private Integer msgFmt;
    private String serviceId;
    private String channelCode;
    private Integer status;
    private String statusReport;
    private String errorMsg;
    private String signature;
    private String carrier;
    private BigDecimal fee;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime statusReportTime;

    // ===== 通道信息 =====
    private String channelName;
    private String channelHost;
    private Integer channelPort;
    private Integer channelStatus;
}
