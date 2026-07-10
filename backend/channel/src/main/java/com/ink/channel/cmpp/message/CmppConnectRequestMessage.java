package com.ink.channel.cmpp.message;

import com.ink.channel.cmpp.CmppCommandType;
import com.ink.channel.cmpp.util.CmppAuthUtil;
import lombok.Data;

import java.nio.charset.StandardCharsets;

/**
 * CMPP Connect 请求消息
 * 消息体：SourceAddr(6) + AuthenticatorClient(16) + Version(1) + Timestamp(4) = 27 字节
 */
@Data
public class CmppConnectRequestMessage {

    /** SP 企业代码（6 字节，不足右补 0x00） */
    private String sourceAddr;

    /** 认证信息（16 字节 MD5） */
    private byte[] authenticatorClient;

    /** 版本号（高 4 位为主版本，低 4 位为次版本） */
    private byte version;

    /** 时间戳（MMDDHHMMSS，10 字节 ASCII） */
    private String timestamp;

    /**
     * 创建 Connect 请求
     */
    public static CmppConnectRequestMessage create(String spId, String secret, byte version) {
        CmppConnectRequestMessage msg = new CmppConnectRequestMessage();
        msg.setSourceAddr(spId);
        msg.setVersion(version);
        msg.setTimestamp(CmppAuthUtil.getCurrentTimestamp());
        msg.setAuthenticatorClient(CmppAuthUtil.generateAuthenticatorClient(spId, secret, msg.getTimestamp()));
        return msg;
    }

    /**
     * 序列化为字节数组
     */
    public byte[] toBytes() {
        byte[] body = new byte[27];

        // SourceAddr (6 bytes)
        byte[] spIdBytes = sourceAddr.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(spIdBytes, 0, body, 0, Math.min(spIdBytes.length, 6));

        // AuthenticatorClient (16 bytes)
        if (authenticatorClient != null) {
            System.arraycopy(authenticatorClient, 0, body, 6, Math.min(authenticatorClient.length, 16));
        }

        // Version (1 byte)
        body[22] = version;

        // Timestamp (4 bytes - MMDDHHMMSS as int)
        byte[] tsBytes = timestamp.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(tsBytes, 0, body, 23, Math.min(tsBytes.length, 4));

        return body;
    }

    /**
     * 从字节数组解析
     */
    public static CmppConnectRequestMessage fromBytes(byte[] body) {
        CmppConnectRequestMessage msg = new CmppConnectRequestMessage();

        // SourceAddr (6 bytes)
        byte[] spIdBytes = new byte[6];
        System.arraycopy(body, 0, spIdBytes, 0, 6);
        msg.setSourceAddr(new String(spIdBytes, StandardCharsets.US_ASCII).trim());

        // AuthenticatorClient (16 bytes)
        byte[] authBytes = new byte[16];
        System.arraycopy(body, 6, authBytes, 0, 16);
        msg.setAuthenticatorClient(authBytes);

        // Version (1 byte)
        msg.setVersion(body[22]);

        // Timestamp (4 bytes)
        byte[] tsBytes = new byte[4];
        System.arraycopy(body, 23, tsBytes, 0, 4);
        msg.setTimestamp(new String(tsBytes, StandardCharsets.US_ASCII).trim());

        return msg;
    }

    public int getCommandId() {
        return CmppCommandType.CONNECT.getCommandId();
    }
}
