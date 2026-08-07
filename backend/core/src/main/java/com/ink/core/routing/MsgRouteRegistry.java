package com.ink.core.routing;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 下行消息路由注册表（内存）
 * 记录 serverMsgId -> (发送客户, 通道)，供状态报告到达时路由回执
 * 内存缺失时由 api 侧通过数据库兜底查询
 */
@Slf4j
@Component
public class MsgRouteRegistry {

    /** 容量上限，超出时按时间淘汰最旧 10% */
    private static final int MAX_ENTRIES = 100_000;

    private final ConcurrentHashMap<String, RouteEntry> routes = new ConcurrentHashMap<>();

    /**
     * 路由条目
     */
    @Getter
    public static class RouteEntry {
        private final String spId;
        private final String channelCode;
        private final long timestamp;

        public RouteEntry(String spId, String channelCode, long timestamp) {
            this.spId = spId;
            this.channelCode = channelCode;
            this.timestamp = timestamp;
        }
    }

    /**
     * 登记路由（Submit 响应成功后调用）
     */
    public void register(String serverMsgIdHex, String spId, String channelCode) {
        if (serverMsgIdHex == null || spId == null) {
            return;
        }
        if (routes.size() >= MAX_ENTRIES) {
            evictOldest();
        }
        routes.put(serverMsgIdHex, new RouteEntry(spId, channelCode, System.currentTimeMillis()));
    }

    /**
     * 查询路由
     */
    public RouteEntry lookup(String serverMsgIdHex) {
        return serverMsgIdHex == null ? null : routes.get(serverMsgIdHex);
    }

    /**
     * 淘汰最旧 10% 条目
     */
    private void evictOldest() {
        List<String> oldest = routes.entrySet().stream()
                .sorted(Comparator.comparingLong(e -> e.getValue().getTimestamp()))
                .limit(MAX_ENTRIES / 10L)
                .map(Map.Entry::getKey)
                .toList();
        oldest.forEach(routes::remove);
        log.info("路由注册表达到上限，已淘汰 {} 条旧记录", oldest.size());
    }
}
