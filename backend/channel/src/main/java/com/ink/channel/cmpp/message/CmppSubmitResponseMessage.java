package com.ink.channel.cmpp.message;

import com.ink.channel.cmpp.CmppCommandType;
import lombok.Data;

/**
 * CMPP Submit 响应消息
 * CMPP 2.0 消息体：Msg_Id(8) + Result(1) = 9 字节
 * CMPP 3.0 消息体：Msg_Id(8) + Result(4) = 12 字节
 */
@Data
public class CmppSubmitResponseMessage {

    /** 消息标识（8 字节） */
    private long msgId;

    /** 结果：0=正确，1=结构错，2=计费错，3=MS 不存在，... */
    private int result;

    public static CmppSubmitResponseMessage fromBytes(byte[] body) {
        CmppSubmitResponseMessage msg = new CmppSubmitResponseMessage();
        if (body == null || body.length < 9) {
            msg.setResult(-1);
            return msg;
        }

        // Msg_Id (8 bytes)
        long id = 0;
        for (int i = 0; i < 8; i++) {
            id = (id << 8) | (body[i] & 0xFF);
        }
        msg.setMsgId(id);

        // Result: CMPP 2.0 = 1 byte, CMPP 3.0 = 4 bytes
        if (body.length >= 12) {
            msg.setResult(((body[8] & 0xFF) << 24) | ((body[9] & 0xFF) << 16)
                    | ((body[10] & 0xFF) << 8) | (body[11] & 0xFF));
        } else {
            msg.setResult(body[8] & 0xFF);
        }

        return msg;
    }

    public byte[] toBytes() {
        // 默认输出 CMPP 2.0 格式：Msg_Id(8) + Result(1) = 9 字节
        byte[] body = new byte[9];
        // Msg_Id
        long id = msgId;
        for (int i = 7; i >= 0; i--) {
            body[i] = (byte) (id & 0xFF);
            id >>= 8;
        }
        // Result (1 byte for CMPP 2.0)
        body[8] = (byte) (result & 0xFF);
        return body;
    }

    public int getCommandId() {
        return CmppCommandType.SUBMIT_RESP.getCommandId();
    }

    public boolean isSuccess() {
        return result == 0;
    }
}
