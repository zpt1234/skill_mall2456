package com.seckill.util;

import java.util.regex.Pattern;

public class PhoneUtil {
    // 国内手机号正则 11位
    private static final String REGEX = "^1[3-9]\\d{9}$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    public static boolean isValid(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return PATTERN.matcher(phone.trim()).matches();
    }
}
