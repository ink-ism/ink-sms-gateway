package com.ink.channel.cmpp.message;

import com.ink.channel.cmpp.CmppCommandType;
import lombok.Data;

/**
 * CMPP Connect 响应消息
 * 消息体：Status(4) + AuthenticatorISMG(16) + Version(1) = 21 字节
 */
@Data
public class CmppConnectResponseMessage {

    /** 状态码：0=正确，1=消息结构错，2=非法源地址，3=认证错，4=版本太高，5+=其他 */
    private int status;

    /** ISMG 认证信息（16 字节） */
    private byte[] authenticatorIsmg;

    /** 服务器支持的版本号 */
    private byte version;

    /**
     * 从字节数组解析
     */
    public static CmppConnectResponseMessage fromBytes(byte[] body) {
        CmppConnectResponseMessage msg = new CmppConnectResponseMessage();

        if (body == null || body.length < 4) {
            msg.setStatus(-1);
            return msg;
        }

        // Status (4 bytes, unsigned int)
        msg.setStatus(((body[0] & 0xFF) << 24) | ((body[1] & 0xFF) << 16)
                | ((body[2] & 0xFF) << 8) | (body[3] & 0xFF));

        // AuthenticatorISMG (up to 16 bytes, may be shorter in some implementations)
        if (body.length >= 5) {
            int authLen = Math.min(body.length - 4, 16);
            byte[] authBytes = new byte[authLen];
            System.arraycopy(body, 4, authBytes, 0, authLen);
            msg.setAuthenticatorIsmg(authBytes);
        }

        // Version (1 byte, if present)
        if (body.length >= 21) {
            msg.setVersion(body[20]);
        }

        return msg;
    }

    /**
     * 序列化为字节数组
     */
    public byte[] toBytes() {
        byte[] body = new byte[21];

        // Status (4 bytes)
        body[0] = (byte) (status >> 24);
        body[1] = (byte) (status >> 16);
        body[2] = (byte) (status >> 8);
        body[3] = (byte) status;

        // AuthenticatorISMG (16 bytes)
        if (authenticatorIsmg != null) {
            System.arraycopy(authenticatorIsmg, 0, body, 4, Math.min(authenticatorIsmg.length, 16));
        }

        // Version (1 byte)
        body[20] = version;

        return body;
    }

    public int getCommandId() {
        return CmppCommandType.CONNECT_RESP.getCommandId();
    }

    /**
     * 是否认证成功
     */
    public boolean isSuccess() {
        return status == 0;
    }
}
