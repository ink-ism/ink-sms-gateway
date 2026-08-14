package com.ink.channel.cmpp.message;

import com.ink.channel.cmpp.CmppCommandType;
import com.ink.channel.cmpp.util.CmppAuthUtil;
import lombok.Data;

import java.nio.charset.StandardCharsets;

/**
 * CMPP Connect 请求消息
 * 消息体：SourceAddr(6) + AuthenticatorClient(16) + Version(1) + Timestamp(10) = 33 字节
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
        byte[] body = new byte[33];

        // SourceAddr (6 bytes)
        byte[] spIdBytes = sourceAddr.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(spIdBytes, 0, body, 0, Math.min(spIdBytes.length, 6));

        // AuthenticatorClient (16 bytes)
        if (authenticatorClient != null) {
            System.arraycopy(authenticatorClient, 0, body, 6, Math.min(authenticatorClient.length, 16));
        }

        // Version (1 byte)
        body[22] = version;

        // Timestamp (10 bytes - MMDDHHMMSS ASCII)
        byte[] tsBytes = timestamp.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(tsBytes, 0, body, 23, Math.min(tsBytes.length, 10));

        return body;
    }

    /**
     * 从字节数组解析
     * CMPP 2.0 规范 SourceAddr 为 6 字节，但实际部署中可能更长（兼容长 SP_ID）。
     * 解析策略：从 body 总长度反推 SourceAddr 长度 = body.length - 27
     */
    public static CmppConnectRequestMessage fromBytes(byte[] body) {
        CmppConnectRequestMessage msg = new CmppConnectRequestMessage();
        if (body == null || body.length < 33) {
            return msg;
        }

        // SourceAddr: 动态长度 = body.length - 16(auth) - 1(version) - 10(timestamp)
        int spAddrLen = body.length - 27;
        if (spAddrLen < 6) spAddrLen = 6; // 至少 6 字节
        byte[] spIdBytes = new byte[spAddrLen];
        System.arraycopy(body, 0, spIdBytes, 0, spAddrLen);
        msg.setSourceAddr(readFixedString(spIdBytes, 0, spAddrLen));

        // AuthenticatorClient (16 bytes)
        byte[] authBytes = new byte[16];
        System.arraycopy(body, spAddrLen, authBytes, 0, 16);
        msg.setAuthenticatorClient(authBytes);

        // Version (1 byte)
        msg.setVersion(body[spAddrLen + 16]);

        // Timestamp (10 bytes)
        byte[] tsBytes = new byte[10];
        System.arraycopy(body, spAddrLen + 17, tsBytes, 0, 10);
        msg.setTimestamp(new String(tsBytes, StandardCharsets.US_ASCII).trim());

        return msg;
    }

    /** 读取固定长度字符串（去除尾部 0x00） */
    private static String readFixedString(byte[] buf, int pos, int length) {
        if (pos + length > buf.length) return "";
        int end = pos;
        while (end < pos + length && buf[end] != 0) end++;
        return new String(buf, pos, end - pos, StandardCharsets.US_ASCII).trim();
    }

    public int getCommandId() {
        return CmppCommandType.CONNECT.getCommandId();
    }
}
