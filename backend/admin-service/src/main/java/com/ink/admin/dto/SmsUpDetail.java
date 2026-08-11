package com.ink.admin.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 上行短信详情 DTO（含关联下行短信与通道信息）
 */
@Data
public class SmsUpDetail {

    // ===== 上行短信信息 =====
    private Long id;
    private String msgId;
    private String spId;
    private String srcTerminalId;
    private String destId;
    private String msgContent;
    private Integer msgFmt;
    private String serviceId;
    private Integer isReport;
    private String reportStat;
    private String channelCode;
    private LocalDateTime createTime;

    // ===== 关联下行短信信息 =====
    private String downMsgContent;
    private LocalDateTime downCreateTime;

    // ===== 通道信息 =====
    private String channelName;
    private String channelHost;
    private Integer channelPort;
    private Integer channelStatus;
}
