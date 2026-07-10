package com.ink.api.service;

import com.ink.core.config.CmppChannelConfig;
import com.ink.core.config.ChannelConfigLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 从数据库 ink_channel 表加载启用的通道配置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbChannelConfigLoader implements ChannelConfigLoader {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<CmppChannelConfig> loadEnabledChannels() {
        List<CmppChannelConfig> configs = new ArrayList<>();
        try {
            jdbcTemplate.query(
                    "SELECT code, host, port, sp_id, shared_secret, version, " +
                    "heartbeat_interval, reconnect_interval, max_reconnect_interval, " +
                    "connect_timeout, max_concurrent " +
                    "FROM ink_channel WHERE status = 1",
                    rs -> {
                        CmppChannelConfig config = new CmppChannelConfig();
                        config.setChannelCode(rs.getString("code"));
                        config.setHost(rs.getString("host"));
                        config.setPort(rs.getInt("port"));
                        config.setSpId(rs.getString("sp_id"));
                        config.setSharedSecret(rs.getString("shared_secret"));
                        config.setVersion(rs.getInt("version"));
                        config.setHeartbeatInterval(rs.getInt("heartbeat_interval"));
                        config.setReconnectInterval(rs.getInt("reconnect_interval"));
                        config.setMaxReconnectInterval(rs.getInt("max_reconnect_interval"));
                        config.setConnectTimeout(rs.getInt("connect_timeout"));
                        config.setMaxConcurrent(rs.getInt("max_concurrent"));
                        configs.add(config);
                    }
            );
        } catch (Exception e) {
            log.error("加载通道配置失败: {}", e.getMessage(), e);
        }
        log.info("从数据库加载 {} 个启用通道", configs.size());
        return configs;
    }
}
