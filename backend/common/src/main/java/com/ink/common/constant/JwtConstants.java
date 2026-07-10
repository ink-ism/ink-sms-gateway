package com.ink.common.constant;

/**
 * JWT 相关常量
 */
public final class JwtConstants {

    private JwtConstants() {}

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String USER_ID_KEY = "userId";
    public static final String USERNAME_KEY = "username";
    public static final long EXPIRATION_TIME = 86400000L; // 24小时
}
