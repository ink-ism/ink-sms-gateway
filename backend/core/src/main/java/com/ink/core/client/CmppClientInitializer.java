package com.ink.core.client;

import com.ink.channel.cmpp.codec.CmppPacketDecoder;
import com.ink.channel.cmpp.codec.CmppPacketEncoder;
import com.ink.core.config.CmppChannelConfig;
import com.ink.core.handler.CmppMessageHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * CMPP 客户端 Pipeline 初始化器
 */
public class CmppClientInitializer extends ChannelInitializer<SocketChannel> {

    private final CmppChannelConfig config;
    private final CmppMessageHandler messageHandler;
    private CmppClientHandler clientHandler;

    public CmppClientInitializer(CmppChannelConfig config, CmppMessageHandler messageHandler) {
        this.config = config;
        this.messageHandler = messageHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // 空闲检测（读超时 = 心跳间隔 * 2，写超时 = 心跳间隔）
        pipeline.addLast("idleState", new IdleStateHandler(
                config.getHeartbeatInterval() * 2L,
                config.getHeartbeatInterval(),
                0, TimeUnit.SECONDS));

        // CMPP 编解码器
        pipeline.addLast("decoder", new CmppPacketDecoder());
        pipeline.addLast("encoder", new CmppPacketEncoder());

        // 业务处理器
        clientHandler = new CmppClientHandler(config, messageHandler);
        pipeline.addLast("handler", clientHandler);
    }

    public CmppClientHandler getClientHandler() {
        return clientHandler;
    }
}
