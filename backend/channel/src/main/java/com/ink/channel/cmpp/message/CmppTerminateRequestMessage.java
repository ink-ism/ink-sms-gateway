package com.ink.channel.cmpp.message;

import com.ink.channel.cmpp.CmppCommandType;

/**
 * CMPP Terminate 退出请求消息
 * 消息体：空（0 字节）
 */
public class CmppTerminateRequestMessage {

    public byte[] toBytes() {
        return new byte[0];
    }

    public int getCommandId() {
        return CmppCommandType.TERMINATE.getCommandId();
    }
}
