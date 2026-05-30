package com.seckill.service;


import com.seckill.dto.UserLoginDTO;
import com.seckill.dto.UserRegisterDTO;
import com.seckill.result.Result;

;

public interface UserService {

    /**
     * 用户注册
     */
    Result<?> register(UserRegisterDTO dto);

    /**
     * 用户登录
     */
    Result<?> login(UserLoginDTO dto);
}
