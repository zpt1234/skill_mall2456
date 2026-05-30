package com.seckill.vo;
/**
 * 登录返回 VO
 */

import lombok.Data;

@Data
public class UserLoginVO {
    /**
     * JWT令牌
     */
    private String token;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;
}
