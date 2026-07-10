package com.ink.core.config;

import java.util.List;

/**
 * 通道配置加载器接口
 * 由 api 模块实现，从数据库加载通道配置
 */
public interface ChannelConfigLoader {

    /**
     * 加载所有启用的通道配置
     */
    List<CmppChannelConfig> loadEnabledChannels();
}
