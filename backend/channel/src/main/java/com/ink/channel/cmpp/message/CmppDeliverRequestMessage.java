package com.ink.channel.cmpp.message;

import com.ink.channel.cmpp.CmppCommandType;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * CMPP Deliver 请求消息（短信下发/上行）
 * CMPP 2.0 消息体：125 + Msg_Length + N*Dest_Id(21) 字节
 */
@Data
public class CmppDeliverRequestMessage {

    private static final Logger log = LoggerFactory.getLogger(CmppDeliverRequestMessage.class);

    /** 消息标识（8 字节） */
    private long msgId;

    /** 目的号码（21 字节） */
    private String destId;

    /** 业务类型（10 字节） */
    private String serviceId;

    /** TP_pid */
    private int tpPid;

    /** TP_udhi */
    private int tpUdhi;

    /** 消息格式（0=ASCII, 8=UCS2, 15=GB2312） */
    private int msgFmt;

    /** 消息来源（10 字节） */
    private String msgSrc;

    /** 资费类别 */
    private int feeUserType;

    /** 资费号码（21 字节） */
    private String feeTerminalId;

    /** 源终端号码（21 字节） */
    private String srcTerminalId;

    /** 源终端类型（1 字节） */
    private int srcTerminalType;

    /** 消息长度 */
    private int msgLength;

    /** 消息内容 */
    private String msgContent;

    /** 是否为状态报告 */
    private boolean isReport;

    /** 状态报告中的原始 Msg_Id（8 字节） */
    private long reportMsgId;

    /** 状态报告中的状态值 */
    private String reportStat;

    /**
     * 从字节数组解析（CMPP 2.0 Deliver，emay 服务器格式）
     *
     * emay 服务器格式说明（无 Msg_Src / Fee 字段）:
     * 公共头部:
     *   Msg_Id(8) Dest_Id(21) Service_Id(10) TP_pid(1) TP_udhi(1) Msg_Fmt(1)
     *   Src_Terminal_Id(21, 固定格式) Src_Terminal_Type(1)
     *
     * 上行短信(TP_udhi=0):
     *   Msg_Length(1) Msg_Content(N)
     *
     * 状态报告(TP_udhi=1):
     *   Msg_Length(1) Msg_Content(N)
     *   内容格式: Msg_Id(8) + Stat(7) + Submit_Time(10) + Done_Time(10) + Dest_Terminal_Id(21) + Sequence_Code(1)
     */
    public static CmppDeliverRequestMessage fromBytes(byte[] body) {
        CmppDeliverRequestMessage msg = new CmppDeliverRequestMessage();
        if (body == null || body.length == 0) {
            log.warn("Deliver 消息体为空");
            return msg;
        }

        log.info("Deliver fromBytes: bodyLen={}, hex={}", body.length,
                bytesToHex(body, Math.min(body.length, 120)));

        int pos = 0;

        // Msg_Id (8 bytes)
        if (!checkPos(body, pos, 8)) return msg;
        long id = 0;
        for (int i = 0; i < 8; i++) {
            id = (id << 8) | (body[pos++] & 0xFF);
        }
        msg.setMsgId(id);
        log.info("  [pos=0->8] Msg_Id=0x{}", Long.toHexString(id));

        // Dest_Id (21 bytes)
        if (!checkPos(body, pos, 21)) return msg;
        log.info("  [pos={}] Dest_Id bytes={}", pos, bytesToHex(body, pos, 21));
        msg.setDestId(readFixedString(body, pos, 21));
        pos += 21;
        log.info("  [pos=8->29] Dest_Id={}", msg.getDestId());

        // Service_Id (10 bytes)
        if (!checkPos(body, pos, 10)) return msg;
        log.info("  [pos={}] Service_Id bytes={}", pos, bytesToHex(body, pos, 10));
        msg.setServiceId(readFixedString(body, pos, 10));
        pos += 10;
        log.info("  [pos=29->39] Service_Id={}", msg.getServiceId());

        // TP_pid (1), TP_udhi (1), Msg_Fmt (1)
        if (!checkPos(body, pos, 3)) return msg;
        msg.setTpPid(body[pos++] & 0xFF);
        msg.setTpUdhi(body[pos++] & 0xFF);
        msg.setMsgFmt(body[pos++] & 0xFF);
        log.info("  [pos=39->42] TP_pid={}, TP_udhi={}, Msg_Fmt={}", msg.getTpPid(), msg.getTpUdhi(), msg.getMsgFmt());

        // ===== emay 格式: Src_Terminal_Id(21) + Src_Terminal_Type(1) =====
        // Src_Terminal_Id (21 bytes, 固定格式)
        if (!checkPos(body, pos, 21)) return msg;
        log.info("  [pos={}] Src_Terminal_Id bytes={}", pos, bytesToHex(body, pos, 21));
        msg.setSrcTerminalId(readFixedString(body, pos, 21));
        pos += 21;

        // Src_Terminal_Type (1 byte)
        if (!checkPos(body, pos, 1)) return msg;
        msg.setSrcTerminalType(body[pos++] & 0xFF);
        log.info("  Src_Terminal_Id={}, Src_Terminal_Type={}", msg.getSrcTerminalId(), msg.getSrcTerminalType());

        // Msg_Length (1 byte)
        if (!checkPos(body, pos, 1)) return msg;
        msg.setMsgLength(body[pos++] & 0xFF);
        log.info("  [pos={}] Msg_Length={}", pos - 1, msg.getMsgLength());

        // Msg_Content
        if (msg.getMsgLength() > 0 && pos + msg.getMsgLength() <= body.length) {
            byte[] contentBytes = new byte[msg.getMsgLength()];
            System.arraycopy(body, pos, contentBytes, 0, msg.getMsgLength());

            // 状态报告检测：TP_udhi=1 且内容长度>=57（标准报告: 8+7+10+10+21+1）
            if (msg.getTpUdhi() == 1 && msg.getMsgLength() >= 15) {
                msg.setReport(true);
                parseReport(contentBytes, msg);
            } else {
                msg.setMsgContent(decodeContent(contentBytes, msg.getMsgFmt()));
            }
        } else if (msg.getTpUdhi() == 1 && pos < body.length) {
            // 状态报告: Msg_Length 可能不准确，尝试用剩余字节解析
            int remaining = body.length - pos;
            if (remaining >= 15) {
                byte[] contentBytes = new byte[remaining];
                System.arraycopy(body, pos, contentBytes, 0, remaining);
                msg.setReport(true);
                parseReport(contentBytes, msg);
            }
        }

        log.info("Deliver 解析完成: msgId=0x{}, destId={}, srcTerminal={}, serviceId={}, msgFmt={}, isReport={}, content={}",
                Long.toHexString(msg.getMsgId()), msg.getDestId(), msg.getSrcTerminalId(),
                msg.getServiceId(), msg.getMsgFmt(), msg.isReport(),
                msg.isReport() ? ("stat=" + msg.getReportStat()) : msg.getMsgContent());

        return msg;
    }

    /**
     * 解析状态报告内容（标准 CMPP 格式）
     * 格式: Msg_Id(8) + Stat(7) + Submit_Time(10) + Done_Time(10) + Dest_Terminal_Id(21) + Sequence_Code(1)
     */
    private static void parseReport(byte[] content, CmppDeliverRequestMessage msg) {
        int pos = 0;
        log.debug("parseReport: contentLen={}, hex={}", content.length,
                bytesToHex(content, Math.min(content.length, 60)));

        // Msg_Id (8 bytes)
        if (content.length < 8) return;
        long reportId = 0;
        for (int i = 0; i < 8; i++) {
            reportId = (reportId << 8) | (content[pos++] & 0xFF);
        }
        msg.setReportMsgId(reportId);
        log.debug("parseReport: reportMsgId=0x{}", Long.toHexString(reportId));

        // Stat (7 bytes)
        if (pos + 7 > content.length) return;
        msg.setReportStat(new String(content, pos, 7, StandardCharsets.US_ASCII).trim());
        pos += 7;
        log.debug("parseReport: stat={}", msg.getReportStat());

        // Submit_Time (10 bytes)
        if (pos + 10 > content.length) return;
        String submitTime = new String(content, pos, 10, StandardCharsets.US_ASCII).trim();
        pos += 10;
        log.debug("parseReport: submitTime={}", submitTime);

        // Done_Time (10 bytes)
        if (pos + 10 > content.length) return;
        String doneTime = new String(content, pos, 10, StandardCharsets.US_ASCII).trim();
        pos += 10;
        log.debug("parseReport: doneTime={}", doneTime);

        // Dest_Terminal_Id (21 bytes)
        if (pos + 21 > content.length) return;
        String destTerminalId = readFixedString(content, pos, 21);
        pos += 21;
        log.debug("parseReport: destTerminalId={}", destTerminalId);

        // Sequence_Code (1 byte)
        if (pos < content.length) {
            int seqCode = content[pos] & 0xFF;
            log.debug("parseReport: sequenceCode={}", seqCode);
        }
    }


    private static boolean checkPos(byte[] body, int pos, int need) {
        if (pos + need > body.length) {
            log.warn("Deliver 消息体长度不足: 需要 pos={}+need={}, 实际 bodyLen={}", pos, need, body.length);
            return false;
        }
        return true;
    }

    private static String readFixedString(byte[] buf, int pos, int length) {
        if (pos + length > buf.length) return "";
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
                default -> new String(bytes, StandardCharsets.UTF_16BE);
            };
        } catch (Exception e) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static String bytesToHex(byte[] bytes, int offset, int maxLen) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < Math.min(bytes.length, offset + maxLen); i++) {
            sb.append(String.format("%02X", bytes[i] & 0xFF));
        }
        return sb.toString();
    }

    private static String bytesToHex(byte[] bytes, int maxLen) {
        return bytesToHex(bytes, 0, maxLen);
    }

    public int getCommandId() {
        return CmppCommandType.DELIVER.getCommandId();
    }
}
