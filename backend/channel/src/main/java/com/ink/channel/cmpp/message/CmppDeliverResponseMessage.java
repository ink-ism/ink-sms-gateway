package com.ink.channel.cmpp.message;

import com.ink.channel.cmpp.CmppCommandType;
import lombok.Data;

/**
 * CMPP Deliver 响应消息
 * 消息体：Msg_Id(8) + Result(4) = 12 字节
 */
@Data
public class CmppDeliverResponseMessage {

    private long msgId;
    private int result;

    public static CmppDeliverResponseMessage fromBytes(byte[] body) {
        CmppDeliverResponseMessage msg = new CmppDeliverResponseMessage();
        if (body == null || body.length < 12) {
            msg.setResult(-1);
            return msg;
        }
        long id = 0;
        for (int i = 0; i < 8; i++) {
            id = (id << 8) | (body[i] & 0xFF);
        }
        msg.setMsgId(id);
        msg.setResult(((body[8] & 0xFF) << 24) | ((body[9] & 0xFF) << 16)
                | ((body[10] & 0xFF) << 8) | (body[11] & 0xFF));
        return msg;
    }

    public byte[] toBytes() {
        byte[] body = new byte[12];
        long id = msgId;
        for (int i = 7; i >= 0; i--) {
            body[i] = (byte) (id & 0xFF);
            id >>= 8;
        }
        body[8] = (byte) (result >> 24);
        body[9] = (byte) (result >> 16);
        body[10] = (byte) (result >> 8);
        body[11] = (byte) result;
        return body;
    }

    public int getCommandId() {
        return CmppCommandType.DELIVER_RESP.getCommandId();
    }
}
