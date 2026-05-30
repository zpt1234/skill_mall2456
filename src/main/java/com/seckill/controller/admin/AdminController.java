package com.seckill.controller.admin;


import com.seckill.dto.AdminLoginDTO;
import com.seckill.dto.AdminRegisterDTO;
import com.seckill.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.seckill.result.Result;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Resource
    private AdminService adminService;

    @PostMapping("/login")
    public Result login(@RequestBody AdminLoginDTO dto) {
        return adminService.login(dto);
    }

    @PostMapping("/register")
    public Result createAdmin(@RequestBody AdminRegisterDTO dto) {
        return adminService.createAdmin(dto);
    }
}

