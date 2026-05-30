package com.seckill.service;


import com.seckill.dto.AdminLoginDTO;
import com.seckill.dto.AdminRegisterDTO;
import com.seckill.result.Result;

public interface AdminService {
    Result login(AdminLoginDTO dto);
    Result createAdmin(AdminRegisterDTO dto);

}
