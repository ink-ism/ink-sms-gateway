package com.ink.admin.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 通道配置实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Channel {

    private Long id;

    /** 通道名称 */
    private String name;

    /** 通道编码（唯一标识） */
    private String code;

    /** 服务器地址 */
    private String host;

    /** 服务器端口 */
    private Integer port;

    /** SP企业代码 */
    private String spId;

    /** 共享密钥 */
    private String sharedSecret;

    /** CMPP版本号 */
    private Integer version;

    /** 心跳间隔（秒） */
    private Integer heartbeatInterval;

    /** 重连间隔（秒） */
    private Integer reconnectInterval;

    /** 最大重连间隔（秒） */
    private Integer maxReconnectInterval;

    /** 连接超时（毫秒） */
    private Integer connectTimeout;

    /** 最大并发数 */
    private Integer maxConcurrent;

    /** 通道成本价（元/条） */
    private BigDecimal costPrice;

    /** 状态：0-禁用，1-启用 */
    private Integer status;

    /** 通道描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
