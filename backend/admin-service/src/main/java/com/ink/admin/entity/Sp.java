package com.ink.admin.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 下游客户实体
 * 每个客户一个 spId，通过 CMPP Connect 认证接入网关
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Sp {

    private Long id;

    /** 客户标识（CMPP Source_Addr，唯一） */
    private String spId;

    /** 共享密钥 */
    private String spSecret;

    /** 客户名称 */
    private String name;

    /** 状态：0-禁用，1-启用 */
    private Integer status;

    /** 客户描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 绑定的通道编码列表（非数据库字段） */
    private List<String> channelCodes;
}
