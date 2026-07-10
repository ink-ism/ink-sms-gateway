package com.ink.core.client;

import com.ink.core.config.CmppChannelConfig;
import com.ink.core.handler.CmppMessageHandler;
import com.ink.core.connection.CmppConnectionManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CMPP 客户端
 * 管理到上游 CMPP 服务器的 Netty 连接
 */
public class CmppClient {

    private static final Logger log = LoggerFactory.getLogger(CmppClient.class);

    private final CmppChannelConfig config;
    private final CmppMessageHandler messageHandler;
    private final EventLoopGroup workerGroup;

    @Getter
    private CmppClientInitializer initializer;

    public CmppClient(CmppChannelConfig config, CmppMessageHandler messageHandler) {
        this.config = config;
        this.messageHandler = messageHandler;
        this.workerGroup = new NioEventLoopGroup(1);
    }

    /**
     * 连接到上游 CMPP 服务器
     */
    public ChannelFuture connect() {
        initializer = new CmppClientInitializer(config, messageHandler);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeout())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(initializer);

        log.info("正在连接 CMPP 服务器: {}:{}", config.getHost(), config.getPort());

        return bootstrap.connect(config.getHost(), config.getPort());
    }

    /**
     * 获取客户端处理器
     */
    public CmppClientHandler getClientHandler() {
        return initializer != null ? initializer.getClientHandler() : null;
    }

    /**
     * 关闭客户端
     */
    public void shutdown() {
        workerGroup.shutdownGracefully();
        log.info("CMPP 客户端已关闭");
    }
}
