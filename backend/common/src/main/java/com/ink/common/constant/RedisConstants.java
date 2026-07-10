package com.ink.common.constant;

/**
 * Redis 相关常量
 */
public final class RedisConstants {

    private RedisConstants() {}

    public static final String USER_TOKEN_PREFIX = "user:token:";
    public static final String USER_INFO_PREFIX = "user:info:";
    public static final String SMS_CODE_PREFIX = "sms:code:";
    public static final int TOKEN_EXPIRE_TIME = 86400;     // 24小时（秒）
    public static final int SMS_CODE_EXPIRE_TIME = 300;    // 5分钟（秒）
}
