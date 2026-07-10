package com.ink.channel.cmpp.codec;

import com.ink.channel.cmpp.CmppConstants;
import com.ink.channel.cmpp.CmppHeader;
import com.ink.channel.cmpp.CmppMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * CMPP 消息解码器
 * 处理 TCP 粘包/拆包问题
 */
public class CmppPacketDecoder extends ByteToMessageDecoder {

    private static final Logger log = LoggerFactory.getLogger(CmppPacketDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 至少需要消息头长度
        while (in.readableBytes() >= CmppConstants.HEADER_LENGTH) {
            // 标记可读位置
            in.markReaderIndex();

            // 读取消息头
            int totalLength = in.readInt();
            int commandId = in.readInt();
            int sequenceId = in.readInt();

            // 校验消息长度
            int bodyLength = totalLength - CmppConstants.HEADER_LENGTH;
            if (bodyLength < 0) {
                log.error("无效的 CMPP 消息长度: totalLength={}, headerLength={}", totalLength, CmppConstants.HEADER_LENGTH);
                ctx.close();
                return;
            }

            // 检查是否有足够的消息体数据
            if (in.readableBytes() < bodyLength) {
                // 数据不够，重置读指针等待更多数据
                in.resetReaderIndex();
                return;
            }

            // 读取消息体
            byte[] body = null;
            if (bodyLength > 0) {
                body = new byte[bodyLength];
                in.readBytes(body);
            }

            // 构建消息
            CmppHeader header = new CmppHeader(totalLength, commandId, sequenceId);
            CmppMessage message = new CmppMessage(header, body);

            if (log.isDebugEnabled()) {
                log.debug("解码 CMPP 消息: commandId=0x{}, seqId={}, totalLen={}, bodyLen={}",
                        String.format("%08X", commandId), sequenceId, totalLength, bodyLength);
            }

            out.add(message);
        }
    }
}
