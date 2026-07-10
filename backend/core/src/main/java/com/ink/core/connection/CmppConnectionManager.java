package com.ink.core.connection;

import com.ink.channel.cmpp.CmppCommandType;
import com.ink.channel.cmpp.CmppMessage;
import com.ink.channel.cmpp.message.CmppSubmitRequestMessage;
import com.ink.core.client.CmppClient;
import com.ink.core.client.CmppClientHandler;
import com.ink.core.config.CmppChannelConfig;
import com.ink.core.config.ChannelConfigLoader;
import com.ink.core.handler.CmppMessageHandler;
import com.ink.channel.session.CmppSession;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CMPP 多通道连接池管理器
 * 从数据库加载通道配置，每个通道建立独立连接池，支持 round-robin 负载均衡
 */
@Slf4j
@Component
public class CmppConnectionManager {

    private final ChannelConfigLoader channelConfigLoader;

    /** 通道池映射: channelCode -> ChannelPool */
    private final ConcurrentHashMap<String, ChannelPool> channelPools = new ConcurrentHashMap<>();

    /** 通道列表（保持顺序用于 round-robin） */
    private final List<String> channelOrder = new CopyOnWriteArrayList<>();

    /** round-robin 计数器 */
    private final AtomicInteger roundRobin = new AtomicInteger(0);

    /** 全局重连调度器 */
    private ScheduledExecutorService reconnectScheduler;

    /** 消息处理器 */
    private CmppMessageHandler messageHandler;

    /** 全局序列号生成器 */
    private final AtomicInteger sequenceGenerator = new AtomicInteger(1);

    public CmppConnectionManager(ChannelConfigLoader channelConfigLoader) {
        this.channelConfigLoader = channelConfigLoader;
    }

    public void setMessageHandler(CmppMessageHandler handler) {
        this.messageHandler = handler;
    }

    @PostConstruct
    public void init() {
        reconnectScheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "cmpp-reconnect");
            t.setDaemon(true);
            return t;
        });

        List<CmppChannelConfig> configs = channelConfigLoader.loadEnabledChannels();
        if (configs.isEmpty()) {
            log.warn("数据库无启用通道，CMPP 连接池为空");
            return;
        }

        log.info("从数据库加载 {} 个启用通道", configs.size());
        for (CmppChannelConfig config : configs) {
            createChannelPool(config);
        }
    }

    /**
     * 热刷新通道配置
     * 对比现有池，新增/删除/更新通道
     */
    public synchronized void refreshChannels() {
        List<CmppChannelConfig> configs = channelConfigLoader.loadEnabledChannels();
        Set<String> newCodes = new HashSet<>();
        for (CmppChannelConfig config : configs) {
            newCodes.add(config.getChannelCode());
        }

        // 删除不再存在的通道
        List<String> toRemove = new ArrayList<>();
        for (String code : channelOrder) {
            if (!newCodes.contains(code)) {
                toRemove.add(code);
            }
        }
        for (String code : toRemove) {
            ChannelPool pool = channelPools.remove(code);
            if (pool != null) {
                pool.shutdown();
                log.info("移除通道: {}", code);
            }
        }
        channelOrder.removeAll(toRemove);

        // 新增或更新通道
        for (CmppChannelConfig config : configs) {
            String code = config.getChannelCode();
            ChannelPool existing = channelPools.get(code);
            if (existing == null) {
                createChannelPool(config);
            } else if (!existing.configEquals(config)) {
                // 配置变更，重建通道池
                existing.shutdown();
                createChannelPool(config);
                log.info("重建通道（配置变更）: {}", code);
            }
        }

        log.info("通道刷新完成，当前 {} 个启用通道", channelPools.size());
    }

    /**
     * Submit 结果，包含服务端消息 ID 和使用的通道编码
     */
    @Data
    public static class SubmitResult {
        private final long serverMsgId;
        private final String channelCode;

        public SubmitResult(long serverMsgId, String channelCode) {
            this.serverMsgId = serverMsgId;
            this.channelCode = channelCode;
        }
    }

    /**
     * 发送短信（Submit）
     * round-robin 选通道，再在通道池内 round-robin 选连接
     */
    public CompletableFuture<SubmitResult> submit(CmppSubmitRequestMessage submitReq) {
        PoolEntry entry = pickAvailableEntry();
        if (entry == null) {
            return CompletableFuture.failedFuture(new RuntimeException("CMPP 无可用通道"));
        }

        int seqId = generateSequenceId();
        String channelCode = entry.channelCode;
        CompletableFuture<Long> innerFuture = new CompletableFuture<>();
        entry.clientHandler.registerPendingSubmit(seqId, innerFuture);

        CmppMessage message = CmppMessage.create(
                CmppCommandType.SUBMIT.getCommandId(), seqId, submitReq.toBytes());

        entry.clientHandler.getSession().send(message);
        log.debug("发送 Submit: channel={}, connId={}, seqId={}, dest={}",
                channelCode, entry.connIndex, seqId, submitReq.getDestTerminalId());

        return innerFuture.thenApply(serverMsgId -> new SubmitResult(serverMsgId, channelCode));
    }

    public boolean isConnected() {
        return channelPools.values().stream().anyMatch(ChannelPool::hasConnected);
    }

    public CmppSession getSession() {
        for (ChannelPool pool : channelPools.values()) {
            CmppSession session = pool.getAnySession();
            if (session != null) return session;
        }
        return null;
    }

    public int getPoolSize() {
        return channelPools.values().stream().mapToInt(p -> p.pool.size()).sum();
    }

    public int getConnectedCount() {
        return (int) channelPools.values().stream()
                .flatMap(p -> p.pool.stream())
                .filter(PoolEntry::isConnected)
                .count();
    }

    /**
     * 获取各通道状态
     */
    public Map<String, Map<String, Object>> getChannelStatus() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (String code : channelOrder) {
            ChannelPool pool = channelPools.get(code);
            if (pool != null) {
                Map<String, Object> status = new LinkedHashMap<>();
                status.put("channelCode", code);
                status.put("host", pool.config.getHost());
                status.put("port", pool.config.getPort());
                status.put("poolSize", pool.pool.size());
                status.put("connected", (int) pool.pool.stream().filter(PoolEntry::isConnected).count());
                result.put(code, status);
            }
        }
        return result;
    }

    @PreDestroy
    public void shutdown() {
        for (ChannelPool pool : channelPools.values()) {
            pool.shutdown();
        }
        if (reconnectScheduler != null) {
            reconnectScheduler.shutdown();
        }
        log.info("CMPP 多通道连接管理器已关闭");
    }

    // ==================== 内部方法 ====================

    private void createChannelPool(CmppChannelConfig config) {
        ChannelPool pool = new ChannelPool(config);
        channelPools.put(config.getChannelCode(), pool);
        channelOrder.add(config.getChannelCode());
        log.info("创建通道池: code={}, host={}, poolSize={}",
                config.getChannelCode(), config.getHost(), config.getMaxConcurrent());
    }

    private PoolEntry pickAvailableEntry() {
        if (channelOrder.isEmpty()) return null;

        int chSize = channelOrder.size();
        int chStart = roundRobin.getAndIncrement() % chSize;
        if (chStart < 0) chStart += chSize;

        // 尝试每个通道
        for (int c = 0; c < chSize; c++) {
            int chIdx = (chStart + c) % chSize;
            ChannelPool pool = channelPools.get(channelOrder.get(chIdx));
            if (pool == null) continue;

            // 在通道池内 round-robin 选连接
            PoolEntry entry = pool.pickAvailable();
            if (entry != null) return entry;
        }
        return null;
    }

    private int generateSequenceId() {
        int seq = sequenceGenerator.getAndIncrement();
        if (seq > 0xFFFFFF) {
            sequenceGenerator.set(1);
            return 1;
        }
        return seq;
    }

    // ==================== 内部类：通道池 ====================

    private class ChannelPool {
        final CmppChannelConfig config;
        final List<PoolEntry> pool = new ArrayList<>();
        final AtomicInteger poolRoundRobin = new AtomicInteger(0);

        ChannelPool(CmppChannelConfig config) {
            this.config = config;
            int size = Math.max(1, config.getMaxConcurrent());
            for (int i = 0; i < size; i++) {
                PoolEntry entry = new PoolEntry(config.getChannelCode(), i, config);
                pool.add(entry);
                entry.connectAsync();
            }
        }

        boolean hasConnected() {
            return pool.stream().anyMatch(PoolEntry::isConnected);
        }

        CmppSession getAnySession() {
            for (PoolEntry entry : pool) {
                if (entry.isConnected() && entry.clientHandler.getSession() != null) {
                    return entry.clientHandler.getSession();
                }
            }
            return null;
        }

        PoolEntry pickAvailable() {
            int size = pool.size();
            int start = poolRoundRobin.getAndIncrement() % size;
            if (start < 0) start += size;
            for (int i = 0; i < size; i++) {
                PoolEntry entry = pool.get((start + i) % size);
                if (entry.isConnected()) return entry;
            }
            return null;
        }

        boolean configEquals(CmppChannelConfig other) {
            return Objects.equals(config.getHost(), other.getHost())
                    && config.getPort() == other.getPort()
                    && Objects.equals(config.getSpId(), other.getSpId())
                    && Objects.equals(config.getSharedSecret(), other.getSharedSecret())
                    && config.getVersion() == other.getVersion()
                    && config.getMaxConcurrent() == other.getMaxConcurrent();
        }

        void shutdown() {
            for (PoolEntry entry : pool) {
                entry.shutdown();
            }
        }
    }

    // ==================== 内部类：连接池条目 ====================

    private class PoolEntry {
        final String channelCode;
        final int connIndex;
        final CmppChannelConfig config;
        volatile CmppClient client;
        volatile CmppClientHandler clientHandler;
        volatile boolean connected = false;
        ScheduledExecutorService heartbeatScheduler;
        final AtomicInteger reconnectInterval = new AtomicInteger(0);

        PoolEntry(String channelCode, int connIndex, CmppChannelConfig config) {
            this.channelCode = channelCode;
            this.connIndex = connIndex;
            this.config = config;
        }

        boolean isConnected() {
            return connected && clientHandler != null
                    && clientHandler.getSession() != null
                    && clientHandler.getSession().isAuthenticated();
        }

        void connectAsync() {
            CompletableFuture.runAsync(this::connect);
        }

        synchronized void connect() {
            if (connected) return;
            try {
                CmppMessageHandler handler = createMessageHandler();
                client = new CmppClient(config, handler);
                client.connect().addListener(future -> {
                    if (future.isSuccess()) {
                        log.info("CMPP 连接[{}][{}]建立成功: {}:{}",
                                channelCode, connIndex, config.getHost(), config.getPort());
                        clientHandler = client.getClientHandler();
                        connected = true;
                        reconnectInterval.set(config.getReconnectInterval());
                        startHeartbeat();
                    } else {
                        log.error("CMPP 连接[{}][{}]建立失败: {}",
                                channelCode, connIndex, future.cause().getMessage());
                        scheduleReconnect();
                    }
                });
            } catch (Exception e) {
                log.error("CMPP 连接[{}][{}]异常: {}", channelCode, connIndex, e.getMessage(), e);
                scheduleReconnect();
            }
        }

        void startHeartbeat() {
            if (heartbeatScheduler != null) heartbeatScheduler.shutdown();
            heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "cmpp-hb-" + channelCode + "-" + connIndex);
                t.setDaemon(true);
                return t;
            });
            heartbeatScheduler.scheduleAtFixedRate(() -> {
                try {
                    if (clientHandler != null && isConnected()) {
                        clientHandler.sendHeartbeat();
                    }
                } catch (Exception e) {
                    log.error("CMPP 连接[{}][{}]心跳异常: {}", channelCode, connIndex, e.getMessage());
                }
            }, config.getHeartbeatInterval(), config.getHeartbeatInterval(), TimeUnit.SECONDS);
        }

        void scheduleReconnect() {
            int interval = reconnectInterval.getAndUpdate(prev -> {
                int next = prev == 0 ? config.getReconnectInterval() : Math.min(prev * 2, config.getMaxReconnectInterval());
                return next;
            });
            log.info("CMPP 连接[{}][{}]将在 {} 秒后重连", channelCode, connIndex, interval);
            reconnectScheduler.schedule(() -> {
                connected = false;
                if (client != null) client.shutdown();
                connect();
            }, interval, TimeUnit.SECONDS);
        }

        CmppMessageHandler createMessageHandler() {
            return new CmppMessageHandler() {
                @Override
                public void handleMessage(CmppMessage message) {
                    if (messageHandler != null) messageHandler.handleMessage(message);
                }
                @Override
                public void onConnected() {
                    connected = true;
                    reconnectInterval.set(config.getReconnectInterval());
                    if (messageHandler != null) messageHandler.onConnected();
                }
                @Override
                public void onDisconnected() {
                    connected = false;
                    log.warn("CMPP 连接[{}][{}]断开", channelCode, connIndex);
                    if (messageHandler != null) messageHandler.onDisconnected();
                    scheduleReconnect();
                }
            };
        }

        void shutdown() {
            if (clientHandler != null) clientHandler.sendTerminate();
            if (client != null) client.shutdown();
            if (heartbeatScheduler != null) heartbeatScheduler.shutdown();
        }
    }
}
