package com.ink.channel.cmpp.message;

import com.ink.channel.cmpp.CmppCommandType;

/**
 * CMPP ActiveTest 心跳请求消息
 * 消息体：空（0 字节）
 */
public class CmppActiveTestRequestMessage {

    public byte[] toBytes() {
        return new byte[0];
    }

    public int getCommandId() {
        return CmppCommandType.ACTIVE_TEST.getCommandId();
    }
}
