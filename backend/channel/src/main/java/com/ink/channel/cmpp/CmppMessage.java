package com.ink.channel.cmpp;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * CMPP 基础消息类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CmppMessage {

    /** 消息头 */
    private CmppHeader header;

    /** 消息体（原始字节） */
    private byte[] body;

    /**
     * 创建指定命令类型的消息
     */
    public static CmppMessage create(int commandId, int sequenceId, byte[] body) {
        CmppHeader header = new CmppHeader();
        header.setCommandId(commandId);
        header.setSequenceId(sequenceId);
        header.setTotalLength(CmppConstants.HEADER_LENGTH + (body != null ? body.length : 0));
        return new CmppMessage(header, body);
    }

    /**
     * 获取命令ID
     */
    public int getCommandId() {
        return header != null ? header.getCommandId() : 0;
    }

    /**
     * 获取序列号
     */
    public int getSequenceId() {
        return header != null ? header.getSequenceId() : 0;
    }
}
