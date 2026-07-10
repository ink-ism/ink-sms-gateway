package com.ink.channel.cmpp;

import lombok.Getter;

/**
 * CMPP 命令字枚举
 */
@Getter
public enum CmppCommandType {

    /** 请求连接 */
    CONNECT(0x00000001, "请求连接"),
    /** 请求连接应答 */
    CONNECT_RESP(0x80000001, "请求连接应答"),
    /** 提交短信 */
    SUBMIT(0x00000004, "提交短信"),
    /** 提交短信应答 */
    SUBMIT_RESP(0x80000004, "提交短信应答"),
    /** 短信下发 */
    DELIVER(0x00000005, "短信下发"),
    /** 短信下发应答 */
    DELIVER_RESP(0x80000005, "短信下发应答"),
    /** 激活测试 */
    ACTIVE_TEST(0x00000008, "激活测试"),
    /** 激活测试应答 */
    ACTIVE_TEST_RESP(0x80000008, "激活测试应答"),
    /** 退出请求 */
    TERMINATE(0x00000006, "退出请求"),
    /** 退出应答 */
    TERMINATE_RESP(0x80000006, "退出应答");

    private final int commandId;
    private final String description;

    CmppCommandType(int commandId, String description) {
        this.commandId = commandId;
        this.description = description;
    }

    /**
     * 根据命令字获取枚举
     */
    public static CmppCommandType fromCommandId(int commandId) {
        for (CmppCommandType type : values()) {
            if (type.commandId == commandId) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断是否为应答命令（最高位为1）
     */
    public static boolean isResponse(int commandId) {
        return (commandId & 0x80000000) != 0;
    }
}
