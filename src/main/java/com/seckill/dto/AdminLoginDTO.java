package com.seckill.dto;

import lombok.Data;

/**
 * 管理员登录/创建账号 DTO
 */
@Data
public class AdminLoginDTO {
    // 管理员账号
    private String username;
    // 管理员密码
    private String password;
}
