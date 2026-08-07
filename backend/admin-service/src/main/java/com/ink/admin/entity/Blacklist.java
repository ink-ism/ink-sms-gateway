package com.ink.admin.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 通道级退订黑名单实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Blacklist {

    private Long id;

    /** 通道编码（黑名单按通道隔离） */
    private String channelCode;

    /** 退订手机号 */
    private String phone;

    /** 触发退订的关键字 */
    private String keyword;

    /** 触发退订的上行消息ID */
    private String sourceMoId;

    /** 过期时间（过期后自动失效） */
    private LocalDateTime expireTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
