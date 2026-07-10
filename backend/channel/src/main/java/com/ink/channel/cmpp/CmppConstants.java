package com.ink.channel.cmpp;

/**
 * CMPP 2.0 协议常量定义
 */
public final class CmppConstants {

    private CmppConstants() {}

    /** 消息头长度（字节）：TotalLength(4) + CommandId(4) + SequenceId(4) */
    public static final int HEADER_LENGTH = 12;

    /** CMPP 2.0 版本号 */
    public static final byte VERSION = 0x20;

    /** 序列号起始值 */
    public static final int SEQUENCE_START = 1;

    /** 序列号最大值（3字节） */
    public static final int SEQUENCE_MAX = 0xFFFFFF;

    /** 心跳间隔（秒） */
    public static final int HEARTBEAT_INTERVAL = 60;

    /** 重连间隔（秒） */
    public static final int RECONNECT_INTERVAL = 10;

    /** 重连最大间隔（秒） */
    public static final int RECONNECT_MAX_INTERVAL = 60;

    /** 认证时间戳格式：MMDDHHMMSS */
    public static final String TIMESTAMP_FORMAT = "MMddHHmmss";
}
