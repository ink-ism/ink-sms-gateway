package com.ink.channel.cmpp.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * CMPP 认证工具类
 * 密码算法：MD5(SP_ID + 9字节0 + shared_secret + timestamp)
 * timestamp 格式：MMDDHHMMSS
 */
public final class CmppAuthUtil {

    private CmppAuthUtil() {}

    /**
     * 生成 CMPP Connect 认证密码
     *
     * @param spId     SP 企业代码
     * @param secret   共享密钥
     * @param timestamp 时间戳（MMDDHHMMSS）
     * @return 16 字节 MD5 摘要
     */
    public static byte[] generateAuthenticatorClient(String spId, String secret, String timestamp) {
        try {
            byte[] spIdBytes = spId.getBytes(StandardCharsets.US_ASCII);
            byte[] reservedBytes = new byte[9]; // 9 字节 0
            byte[] secretBytes = secret.getBytes(StandardCharsets.US_ASCII);
            byte[] timestampBytes = timestamp.getBytes(StandardCharsets.US_ASCII);

            // 拼接：SP_ID(6) + 9字节0 + shared_secret + timestamp(10)
            int totalLen = spIdBytes.length + reservedBytes.length + secretBytes.length + timestampBytes.length;
            byte[] data = new byte[totalLen];
            int pos = 0;

            System.arraycopy(spIdBytes, 0, data, pos, spIdBytes.length);
            pos += spIdBytes.length;
            System.arraycopy(reservedBytes, 0, data, pos, reservedBytes.length);
            pos += reservedBytes.length;
            System.arraycopy(secretBytes, 0, data, pos, secretBytes.length);
            pos += secretBytes.length;
            System.arraycopy(timestampBytes, 0, data, pos, timestampBytes.length);

            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * 生成服务端认证密码（用于验证 SP 连接）
     * 算法与客户端相同
     */
    public static byte[] generateAuthenticatorServer(String spId, String secret, String timestamp) {
        return generateAuthenticatorClient(spId, secret, timestamp);
    }

    /**
     * 获取当前时间戳字符串（MMDDHHMMSS）
     */
    public static String getCurrentTimestamp() {
        return new SimpleDateFormat("MMddHHmmss").format(new Date());
    }

    /**
     * 比较两个认证密码是否一致
     */
    public static boolean verifyAuthenticator(byte[] expected, byte[] actual) {
        if (expected == null || actual == null || expected.length != actual.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expected.length; i++) {
            result |= expected[i] ^ actual[i];
        }
        return result == 0;
    }
}
