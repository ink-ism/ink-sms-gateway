package com.ink.channel.cmpp.message;

import com.ink.channel.cmpp.CmppCommandType;
import lombok.Data;

/**
 * CMPP Terminate 退出响应消息
 * 消息体：空（0 字节）
 */
@Data
public class CmppTerminateResponseMessage {

    public static CmppTerminateResponseMessage fromBytes(byte[] body) {
        return new CmppTerminateResponseMessage();
    }

    public byte[] toBytes() {
        return new byte[0];
    }

    public int getCommandId() {
        return CmppCommandType.TERMINATE_RESP.getCommandId();
    }
}
