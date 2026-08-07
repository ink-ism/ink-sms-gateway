package com.ink.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 退订策略配置
 * 上行内容命中关键字时触发通道级黑名单
 */
@Data
@Component
@ConfigurationProperties(prefix = "cmpp.unsubscribe")
public class CmppUnsubscribeConfig {

    /** 退订关键字集合（逗号分隔，匹配时 trim + 忽略大小写精确匹配） */
    private String keywords = "R,RD,TD,T,N,退订,0000";

    /** 黑名单过期天数（惰性过期） */
    private int blacklistExpireDays = 180;

    /**
     * 判断上行内容是否为退订关键字，命中则返回对应关键字，否则返回 null
     */
    public String matchKeyword(String content) {
        if (content == null || keywords == null || keywords.isBlank()) {
            return null;
        }
        String trimmed = content.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        for (String keyword : keywords.split(",")) {
            String k = keyword.trim();
            if (!k.isEmpty() && k.equalsIgnoreCase(trimmed)) {
                return k;
            }
        }
        return null;
    }
}
