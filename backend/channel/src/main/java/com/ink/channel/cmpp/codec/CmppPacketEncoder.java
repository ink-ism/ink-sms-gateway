package com.ink.channel.cmpp.codec;

import com.ink.channel.cmpp.CmppConstants;
import com.ink.channel.cmpp.CmppMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CMPP 消息编码器
 */
public class CmppPacketEncoder extends MessageToByteEncoder<CmppMessage> {

    private static final Logger log = LoggerFactory.getLogger(CmppPacketEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, CmppMessage msg, ByteBuf out) {
        int totalLength = CmppConstants.HEADER_LENGTH + (msg.getBody() != null ? msg.getBody().length : 0);

        // 写入消息头
        out.writeInt(totalLength);
        out.writeInt(msg.getHeader().getCommandId());
        out.writeInt(msg.getHeader().getSequenceId());

        // 写入消息体
        if (msg.getBody() != null && msg.getBody().length > 0) {
            out.writeBytes(msg.getBody());
        }

        if (log.isDebugEnabled()) {
            log.debug("编码 CMPP 消息: commandId=0x{}, seqId={}, totalLen={}",
                    String.format("%08X", msg.getHeader().getCommandId()),
                    msg.getHeader().getSequenceId(), totalLength);
        }
    }
}
