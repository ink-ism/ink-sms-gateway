package com.ink.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CMPP 下游服务器配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "cmpp.server")
public class CmppServerConfig {

    /** 监听端口（接受下游 SP 连接） */
    private int port = 7891;

    /** 允许的 SP 列表（逗号分隔，格式：spId:secret） */
    private String allowedSpList = "test:123456";

    /** Boss 线程数 */
    private int bossThreads = 1;

    /** Worker 线程数 */
    private int workerThreads = 4;
}
