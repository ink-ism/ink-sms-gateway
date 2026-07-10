package com.ink.channel.cmpp;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * CMPP 消息头
 * 固定 12 字节：TotalLength(4) + CommandId(4) + SequenceId(4)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CmppHeader {

    /** 消息总长度（含消息头） */
    private int totalLength;

    /** 命令标识 */
    private int commandId;

    /** 消息流水号 */
    private int sequenceId;

    /**
     * 获取命令类型枚举
     */
    public CmppCommandType getCommandType() {
        return CmppCommandType.fromCommandId(commandId);
    }

    /**
     * 是否为应答消息
     */
    public boolean isResponse() {
        return CmppCommandType.isResponse(commandId);
    }
}
