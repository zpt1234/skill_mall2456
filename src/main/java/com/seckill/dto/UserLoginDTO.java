package com.seckill.dto;
import lombok.Data;

/**
 * 接收前端注册参数
 */
@Data
public class UserLoginDTO {
    /**
     * 账号/用户名
     */
    private String username;

    /**
     * 明文密码
     */
    private String password;
}
