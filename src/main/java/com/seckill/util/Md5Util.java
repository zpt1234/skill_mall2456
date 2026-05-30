package com.seckill.util;

import org.springframework.util.DigestUtils;

import java.util.UUID;

public class Md5Util {

    /**
     * 生成随机盐值
     */
    public static String createSalt() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * MD5加密：密码 + 盐值
     */
    public static String encrypt(String password, String salt) {
        return DigestUtils.md5DigestAsHex((password + salt).getBytes());
    }

    /**
     * 校验密码是否正确
     */
    public static boolean check(String inputPassword, String dbPassword, String salt) {
        String encrypt = encrypt(inputPassword, salt);
        return encrypt.equals(dbPassword);
    }
}
