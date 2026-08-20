package com.ink.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 签名提取工具
 * 从短信内容中提取签名（【xxx】格式）
 */
public final class SignatureUtil {

    private SignatureUtil() {}

    /** 匹配开头的签名：【xxx】 */
    private static final Pattern LEADING_SIGNATURE = Pattern.compile("^【([^】]+)】");

    /**
     * 从短信内容中提取签名
     * @param content 短信内容
     * @return 签名内容（含【】），无签名时返回 null
     */
    public static String extract(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        Matcher matcher = LEADING_SIGNATURE.matcher(content.trim());
        if (matcher.find()) {
            return matcher.group(); // 返回完整的 【xxx】
        }
        return null;
    }
}
