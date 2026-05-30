package com.seckill.dto;

import lombok.Data;

@Data
public class AdminRegisterDTO {
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
