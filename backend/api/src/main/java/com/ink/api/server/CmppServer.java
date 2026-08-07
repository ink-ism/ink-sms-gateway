package com.ink.api.server;

import com.ink.api.config.CmppServerConfig;
import com.ink.api.service.DownstreamPushService;
import com.ink.api.service.SmsRecordService;
import com.ink.api.service.SpAccountService;
import com.ink.api.session.SpSessionManager;
import com.ink.core.connection.CmppConnectionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * CMPP 下游服务器
 * 监听端口接受 SP 连接
 */
@Slf4j
@Component
public class CmppServer {

    private final CmppServerConfig serverConfig;
    private final SpSessionManager sessionManager;
    private final CmppConnectionManager connectionManager;
    private final SmsRecordService smsRecordService;
    private final SpAccountService spAccountService;
    private final DownstreamPushService pushService;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public CmppServer(CmppServerConfig serverConfig,
                      SpSessionManager sessionManager,
                      CmppConnectionManager connectionManager,
                      SmsRecordService smsRecordService,
                      SpAccountService spAccountService,
                      DownstreamPushService pushService) {
        this.serverConfig = serverConfig;
        this.sessionManager = sessionManager;
        this.connectionManager = connectionManager;
        this.smsRecordService = smsRecordService;
        this.spAccountService = spAccountService;
        this.pushService = pushService;
    }

    @PostConstruct
    public void start() {
        // 设置静态引用，供 CmppServerHandler 使用
        CmppServerHandler.setConnectionManager(connectionManager);
        CmppServerHandler.setSmsRecordService(smsRecordService);
        CmppServerHandler.setSpAccountService(spAccountService);
        CmppServerHandler.setPushService(pushService);

        bossGroup = new NioEventLoopGroup(serverConfig.getBossThreads());
        workerGroup = new NioEventLoopGroup(serverConfig.getWorkerThreads());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new CmppServerInitializer(serverConfig, sessionManager));

            serverChannel = bootstrap.bind(serverConfig.getPort()).sync().channel();
            log.info("CMPP 下游服务器已启动，监听端口: {}", serverConfig.getPort());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("CMPP 服务器启动失败", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("CMPP 下游服务器已关闭");
    }
}
