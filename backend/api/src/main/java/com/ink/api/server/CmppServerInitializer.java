package com.ink.api.server;

import com.ink.api.config.CmppServerConfig;
import com.ink.api.session.SpSessionManager;
import com.ink.channel.cmpp.codec.CmppPacketDecoder;
import com.ink.channel.cmpp.codec.CmppPacketEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * CMPP 服务端 Pipeline 初始化器
 */
public class CmppServerInitializer extends ChannelInitializer<SocketChannel> {

    private final CmppServerConfig serverConfig;
    private final SpSessionManager sessionManager;

    public CmppServerInitializer(CmppServerConfig serverConfig, SpSessionManager sessionManager) {
        this.serverConfig = serverConfig;
        this.sessionManager = sessionManager;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // 空闲检测
        pipeline.addLast("idleState", new IdleStateHandler(120, 0, 0, TimeUnit.SECONDS));

        // CMPP 编解码器
        pipeline.addLast("decoder", new CmppPacketDecoder());
        pipeline.addLast("encoder", new CmppPacketEncoder());

        // 业务处理器
        pipeline.addLast("handler", new CmppServerHandler(serverConfig, sessionManager));
    }
}
