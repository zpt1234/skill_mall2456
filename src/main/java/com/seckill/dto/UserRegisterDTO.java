package com.seckill.dto;

import lombok.Data;

/**
 * 用户注册接收参数DTO
 */
@Data
public class UserRegisterDTO {
    /**
     * 用户名/账号
     */
    private String username;

    /**
     * 明文密码
     */
    private String password;

    /**
     * 手机号
     */
    private String phone;
}
