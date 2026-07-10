package com.ink.channel.cmpp.message;

import com.ink.channel.cmpp.CmppCommandType;
import lombok.Data;

import java.nio.charset.StandardCharsets;

/**
 * CMPP Submit 请求消息（提交短信）
 * CMPP 2.0 消息体长度：126 + Msg_Length + N*msgContent 字节
 */
@Data
public class CmppSubmitRequestMessage {

    /** 消息流水号（SP 自生成） */
    private int msgId;

    /** 业务类型（10 字节） */
    private String pkTotal = "01";

    /** 相同 msg 的编号 */
    private String pkNumber = "01";

    /** 是否要求状态报告 */
    private int registeredDelivery = 0;

    /** 消息级别 */
    private int msgLevel = 0;

    /** 业务类型（10 字节） */
    private String serviceId = "0000000000";

    /** 资费类别 */
    private int feeUserType = 2;

    /** 资费号码 */
    private String feeTerminalId = "";

    /** TP_pid */
    private int tpPid = 0;

    /** TP_udhi */
    private int tpUdhi = 0;

    /** 消息格式（0=ASCII, 8=UCS2, 15=GB2312） */
    private int msgFmt = 8;

    /** 消息来源（SP_ID，6 字节） */
    private String msgSrc;

    /** 资费类型 */
    private String feeType = "01";

    /** 资费代码 */
    private String feeCode = "000000";

    /** 有效时间 */
    private String validTime = "";

    /** 定时发送 */
    private String atTime = "";

    /** 源号码 */
    private String srcId;

    /** 接收号码数量 */
    private int destUsrTl;

    /** 接收号码（最多 100 个，每个 21 字节） */
    private String[] destTerminalId;

    /** 消息长度 */
    private int msgLength;

    /** 消息内容 */
    private String msgContent;

    /**
     * 序列化为字节数组（简化版本，单目标号码）
     */
    public byte[] toBytes() {
        byte[] contentBytes = getEncodedContent();
        this.msgLength = contentBytes.length;
        this.destUsrTl = destTerminalId != null ? destTerminalId.length : 0;

        // 计算总长度：固定字段126 + 目标号码21*N + Msg_Length(1) + 消息内容
        int destCount = destTerminalId != null ? destTerminalId.length : 0;
        int bodyLen = 126 + (21 * destCount) + 1 + msgLength;
        byte[] body = new byte[bodyLen];
        int pos = 0;

        // Msg_Id (8 bytes) - 简化为序列号
        pos = writeLong(body, pos, msgId);

        // Pk_total, Pk_number (1+1)
        body[pos++] = 1;
        body[pos++] = 1;

        // Registered_Delivery (1)
        body[pos++] = (byte) registeredDelivery;

        // Msg_Level (1)
        body[pos++] = (byte) msgLevel;

        // Service_Id (10)
        pos = writeFixedString(body, pos, serviceId, 10);

        // Fee_UserType (1)
        body[pos++] = (byte) feeUserType;

        // Fee_Terminal_Id (21) - CMPP 2.0 使用 21 字节
        pos = writeFixedString(body, pos, feeTerminalId, 21);

        // TP_pid (1), TP_udhi (1), Msg_Fmt (1)
        body[pos++] = (byte) tpPid;
        body[pos++] = (byte) tpUdhi;
        body[pos++] = (byte) msgFmt;

        // Msg_Src (6)
        pos = writeFixedString(body, pos, msgSrc, 6);

        // FeeType (2), FeeCode (6)
        pos = writeFixedString(body, pos, feeType, 2);
        pos = writeFixedString(body, pos, feeCode, 6);

        // Valid_Time (17)
        pos = writeFixedString(body, pos, validTime, 17);

        // At_Time (17)
        pos = writeFixedString(body, pos, atTime, 17);

        // Src_Id (21)
        pos = writeFixedString(body, pos, srcId, 21);

        // DestUsr_Tl (1)
        body[pos++] = (byte) destUsrTl;

        // Dest_Terminal_Id (21 * N)
        if (destTerminalId != null) {
            for (String dest : destTerminalId) {
                pos = writeFixedString(body, pos, dest, 21);
            }
        }

        // Msg_Length (1)
        body[pos++] = (byte) msgLength;

        // Msg_Content
        System.arraycopy(contentBytes, 0, body, pos, Math.min(contentBytes.length, msgLength));

        return body;
    }

    private byte[] getEncodedContent() {
        if (msgContent == null) return new byte[0];
        return switch (msgFmt) {
            case 0 -> msgContent.getBytes(StandardCharsets.US_ASCII);
            case 15 -> msgContent.getBytes(java.nio.charset.Charset.forName("GB2312"));
            default -> msgContent.getBytes(StandardCharsets.UTF_16BE); // UCS2
        };
    }

    private int writeFixedString(byte[] buf, int pos, String value, int length) {
        if (value != null) {
            byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(bytes, 0, buf, pos, Math.min(bytes.length, length));
        }
        return pos + length;
    }

    private int writeLong(byte[] buf, int pos, long value) {
        for (int i = 7; i >= 0; i--) {
            buf[pos + i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return pos + 8;
    }

    /**
     * 从字节数组解析
     */
    public static CmppSubmitRequestMessage fromBytes(byte[] body) {
        CmppSubmitRequestMessage msg = new CmppSubmitRequestMessage();
        if (body == null || body.length < 126) {
            return msg;
        }
        int pos = 0;

        // Msg_Id (8 bytes)
        long msgIdVal = 0;
        for (int i = 0; i < 8; i++) {
            msgIdVal = (msgIdVal << 8) | (body[pos++] & 0xFF);
        }
        msg.setMsgId((int) msgIdVal);

        // Pk_total (1), Pk_number (1)
        pos += 2;

        // Registered_Delivery (1)
        msg.setRegisteredDelivery(body[pos++] & 0xFF);

        // Msg_Level (1)
        msg.setMsgLevel(body[pos++] & 0xFF);

        // Service_Id (10)
        msg.setServiceId(readFixedString(body, pos, 10));
        pos += 10;

        // Fee_UserType (1)
        msg.setFeeUserType(body[pos++] & 0xFF);

        // Fee_Terminal_Id (21) - CMPP 2.0
        msg.setFeeTerminalId(readFixedString(body, pos, 21));
        pos += 21;

        // TP_pid (1), TP_udhi (1), Msg_Fmt (1)
        msg.setTpPid(body[pos++] & 0xFF);
        msg.setTpUdhi(body[pos++] & 0xFF);
        msg.setMsgFmt(body[pos++] & 0xFF);

        // Msg_Src (6)
        msg.setMsgSrc(readFixedString(body, pos, 6));
        pos += 6;

        // FeeType (2), FeeCode (6)
        msg.setFeeType(readFixedString(body, pos, 2));
        pos += 2;
        msg.setFeeCode(readFixedString(body, pos, 6));
        pos += 6;

        // Valid_Time (17)
        msg.setValidTime(readFixedString(body, pos, 17));
        pos += 17;

        // At_Time (17)
        msg.setAtTime(readFixedString(body, pos, 17));
        pos += 17;

        // Src_Id (21)
        msg.setSrcId(readFixedString(body, pos, 21));
        pos += 21;

        // DestUsr_Tl (1)
        int destCount = body[pos++] & 0xFF;
        msg.setDestUsrTl(destCount);

        // Dest_Terminal_Id (21 * N)
        String[] dests = new String[destCount];
        for (int i = 0; i < destCount && pos + 21 <= body.length; i++) {
            dests[i] = readFixedString(body, pos, 21);
            pos += 21;
        }
        msg.setDestTerminalId(dests);

        // Msg_Length (1)
        if (pos < body.length) {
            msg.setMsgLength(body[pos++] & 0xFF);
        }

        // Msg_Content
        if (pos < body.length && msg.getMsgLength() > 0) {
            int contentLen = Math.min(msg.getMsgLength(), body.length - pos);
            byte[] contentBytes = new byte[contentLen];
            System.arraycopy(body, pos, contentBytes, 0, contentLen);
            try {
                msg.setMsgContent(decodeContent(contentBytes, msg.getMsgFmt()));
            } catch (Exception e) {
                msg.setMsgContent(new String(contentBytes, StandardCharsets.UTF_8));
            }
        }

        return msg;
    }

    private static String readFixedString(byte[] buf, int pos, int length) {
        if (pos + length > buf.length) {
            return "";
        }
        // 找到第一个 0x00 的位置
        int end = pos;
        while (end < pos + length && buf[end] != 0) {
            end++;
        }
        return new String(buf, pos, end - pos, StandardCharsets.US_ASCII).trim();
    }

    private static String decodeContent(byte[] bytes, int fmt) {
        try {
            return switch (fmt) {
                case 0 -> new String(bytes, StandardCharsets.US_ASCII);
                case 15 -> new String(bytes, "GB2312");
                default -> new String(bytes, StandardCharsets.UTF_16BE); // UCS2
            };
        } catch (Exception e) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public int getCommandId() {
        return CmppCommandType.SUBMIT.getCommandId();
    }
}
