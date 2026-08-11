package com.ink.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 限流配置
 * SP 限速值按客户配置（ink_sp.rate_limit），此处仅配置 REST 入口的全局 IP 限速
 */
@Data
@Component
@ConfigurationProperties(prefix = "cmpp.rate-limit")
public class RateLimitConfig {

    /** REST 入口单 IP 每秒发送上限（0-不限制） */
    private int ipPerSecond = 10;
}
