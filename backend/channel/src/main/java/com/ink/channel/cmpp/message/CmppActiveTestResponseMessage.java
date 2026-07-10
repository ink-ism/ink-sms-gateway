package com.ink.channel.cmpp.message;

import com.ink.channel.cmpp.CmppCommandType;
import lombok.Data;

/**
 * CMPP ActiveTest 心跳响应消息
 * 消息体：Reserved(1) + ErrorCode(1) = 2 字节
 */
@Data
public class CmppActiveTestResponseMessage {

    /** 保留字段 */
    private int reserved;

    /** 错误码：0=正确 */
    private int errorCode;

    public static CmppActiveTestResponseMessage fromBytes(byte[] body) {
        CmppActiveTestResponseMessage msg = new CmppActiveTestResponseMessage();
        if (body != null && body.length >= 2) {
            msg.setReserved(body[0] & 0xFF);
            msg.setErrorCode(body[1] & 0xFF);
        }
        return msg;
    }

    public byte[] toBytes() {
        return new byte[]{(byte) reserved, (byte) errorCode};
    }

    public int getCommandId() {
        return CmppCommandType.ACTIVE_TEST_RESP.getCommandId();
    }

    public boolean isSuccess() {
        return errorCode == 0;
    }
}
