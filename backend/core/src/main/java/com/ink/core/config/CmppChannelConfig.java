package com.ink.core.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * CMPP 通道配置
 * 从数据库 ink_channel 表加载，替代原 YAML 配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CmppChannelConfig {

    /** 通道编码（唯一标识） */
    private String channelCode;

    /** CMPP 服务器地址 */
    private String host;

    /** CMPP 服务器端口 */
    private int port;

    /** SP 企业代码 */
    private String spId;

    /** 共享密钥 */
    private String sharedSecret;

    /** CMPP 版本号 */
    private int version = 0x20;

    /** 心跳间隔（秒） */
    private int heartbeatInterval = 60;

    /** 重连间隔（秒） */
    private int reconnectInterval = 10;

    /** 最大重连间隔（秒） */
    private int maxReconnectInterval = 60;

    /** 连接超时（毫秒） */
    private int connectTimeout = 5000;

    /** 最大并发连接数 */
    private int maxConcurrent = 4;
}
