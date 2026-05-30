package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.seckill.dto.AdminLoginDTO;
import com.seckill.dto.AdminRegisterDTO;
import com.seckill.entity.User;
import com.seckill.mapper.UserMapper;
import com.seckill.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.seckill.result.Result;
import com.seckill.util.JwtUtil;
import com.seckill.util.Md5Util;
import com.seckill.util.PhoneUtil;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private JwtUtil jwtUtil;

    /**
     * 管理员登录
     */
    @Override
    public Result login(AdminLoginDTO dto) {
        // 1. 查询用户
        User user = userMapper.selectOne(new QueryWrapper<User>()
                .eq("username", dto.getUsername()));

        if (user == null) {
            return Result.fail("账号不存在");
        }

        // 2. 必须是管理员 role=2
        if (user.getRole() != 2) {
            return Result.fail("无管理员权限");
        }

        boolean passwordMatch = Md5Util.check(dto.getPassword(), user.getPassword(), user.getSalt());
        if (!passwordMatch) {
            return Result.fail("密码错误");
        }

        // 3. 生成 JWT token
        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getId());
        map.put("username", user.getUsername());
        String token = jwtUtil.generateToken(map);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("username", user.getUsername());
        
        log.info("管理员登录成功, adminId: {}", user.getId());
        return Result.success(result);
    }

    @Override
    public Result createAdmin(AdminRegisterDTO dto) {
        // 1. 判断账号是否已存在
        User exist = userMapper.selectOne(new QueryWrapper<User>()
                .eq("username", dto.getUsername()));
        if (exist != null) {
            return Result.fail("账号已存在");
        }
        String phone = dto.getPhone();
        // 2. 手机号格式校验
        if (!PhoneUtil.isValid(phone)) {
            return Result.fail("手机号格式不合法，请输入11位有效手机号");
        }

        // 3. 生成盐 + 加密密码
        String salt = Md5Util.createSalt();
        String password = Md5Util.encrypt(dto.getPassword(), salt);

        // 4. 创建管理员（role=2）
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(password);
        user.setSalt(salt);
        user.setRole(2);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);
        
        log.info("管理员创建成功, adminId: {}", user.getId());
        return Result.success("管理员创建成功");
    }
}


