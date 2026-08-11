package com.ink.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 上行短信记录实体
 */
@Data
public class SmsUp {

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

    /** 关联下行短信内容（列表查询时 LEFT JOIN 填充） */
    private String downMsgContent;
}
