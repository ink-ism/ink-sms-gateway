package com.ink.admin.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.Map;

/**
 * api 模块 HTTP 客户端
 * 用于通知 api 服务刷新内存数据、查询通道连接状态等
 */
@Slf4j
@Component
public class ApiServiceClient {

    private final RestClient restClient;

    public ApiServiceClient(@Value("${ink.api-service.url:http://localhost:8004}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        log.info("ApiServiceClient 初始化: baseUrl={}", baseUrl);
    }

    /**
     * 通知 api 服务刷新敏感词表
     */
    public void refreshSensitiveWords() {
        try {
            restClient.post().uri("/api/sms/sensitives/refresh").retrieve().body(String.class);
            log.info("已通知 api 服务刷新敏感词");
        } catch (Exception e) {
            log.warn("通知 api 服务刷新敏感词失败（api 重启后会自动加载）: {}", e.getMessage());
        }
    }

    /**
     * 查询 api 服务各通道连接状态
     * @return 通道状态 Map；调用失败返回空 Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> getChannelStatus() {
        try {
            Map<String, Object> result = restClient.get()
                    .uri("/api/sms/channels/status")
                    .retrieve()
                    .body(Map.class);
            if (result != null && result.get("data") instanceof Map) {
                return (Map<String, Map<String, Object>>) result.get("data");
            }
        } catch (Exception e) {
            log.warn("查询 api 服务通道状态失败: {}", e.getMessage());
        }
        return Collections.emptyMap();
    }
}
