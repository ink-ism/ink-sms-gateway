package com.ink.common.utils;

/**
 * 手机号运营商解析工具
 * 根据手机号前缀判断运营商（移动/联通/电信）
 */
public final class PhoneCarrierUtil {

    private PhoneCarrierUtil() {}

    public static final String CHINA_MOBILE = "移动";
    public static final String CHINA_UNICOM = "联通";
    public static final String CHINA_TELECOM = "电信";
    public static final String UNKNOWN = "未知";

    /**
     * 根据手机号解析运营商
     * @param phone 手机号（11位）
     * @return 运营商名称，无法识别时返回 "未知"
     */
    public static String resolve(String phone) {
        if (phone == null || phone.length() < 7) {
            return UNKNOWN;
        }
        // 取前3位判断号段
        String prefix = phone.substring(0, 3);
        // 取前4位判断更精细的号段
        String prefix4 = phone.length() >= 4 ? phone.substring(0, 4) : prefix;

        return switch (prefix) {
            // 中国移动
            case "134", "135", "136", "137", "138", "139",
                 "150", "151", "152", "157", "158", "159",
                 "172", "178",
                 "182", "183", "184", "187", "188",
                 "195", "197", "198" -> CHINA_MOBILE;
            // 中国联通
            case "130", "131", "132",
                 "155", "156",
                 "166", "167",
                 "175", "176",
                 "185", "186",
                 "196" -> CHINA_UNICOM;
            // 中国电信
            case "133", "149", "153",
                 "173", "174", "177",
                 "180", "181", "189",
                 "190", "191", "193", "199" -> CHINA_TELECOM;
            default -> {
                // 189 同时存在于移动和电信号段，优先按电信处理（上面已覆盖）
                // 14x 号段细分
                if ("149".equals(prefix4) || "149".equals(prefix)) yield CHINA_TELECOM;
                if ("141".equals(prefix4) || "142".equals(prefix4) || "144".equals(prefix4)) yield CHINA_MOBILE;
                yield UNKNOWN;
            }
        };
    }
}
