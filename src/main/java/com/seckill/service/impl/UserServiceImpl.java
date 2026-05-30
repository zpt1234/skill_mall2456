package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.dto.UserLoginDTO;
import com.seckill.dto.UserRegisterDTO;
import com.seckill.entity.User;
import com.seckill.mapper.UserMapper;
import com.seckill.service.UserService;
import com.seckill.vo.UserLoginVO;
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
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private JwtUtil jwtUtil;

    @Override
    public Result<?> register(UserRegisterDTO dto) {
        String username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        String password = dto.getPassword() == null ? "" : dto.getPassword().trim();
        String phone = dto.getPhone() == null ? "" : dto.getPhone().trim();

        // 1. 用户名非空校验
        if (username.isEmpty()) {
            return Result.fail("用户名不能为空");
        }
        // 2. 用户名长度校验 4-20位
        if (username.length() < 4 || username.length() > 20) {
            return Result.fail("用户名长度必须在4~20位之间");
        }
        // 3. 密码非空
        if (password.isEmpty()) {
            return Result.fail("密码不能为空");
        }
        // 4. 手机号格式校验
        if (!PhoneUtil.isValid(phone)) {
            return Result.fail("手机号格式不合法，请输入11位有效手机号");
        }

        // ======================
        // 5. 用户名唯一判断
        // ======================
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User existUser = userMapper.selectOne(queryWrapper);
        if (existUser != null) {
            return Result.fail("用户名已被注册，请更换其他用户名");
        }

        // 6. 密码 MD5 + 盐值加密
        String salt = Md5Util.createSalt();
        String encryptPwd = Md5Util.encrypt(password, salt);

        // 7. 封装数据入库
        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptPwd);
        user.setSalt(salt);
        user.setPhone(phone);
        user.setRole(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        
        log.info("用户注册成功, userId: {}, username: {}", user.getId(), username);
        return Result.success("注册成功");
    }

    @Override
    public Result<?> login(UserLoginDTO dto) {
        // 1. 用户名处理
        String username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        if (username.isEmpty()) {
            return Result.fail("用户名不能为空");
        }

        // 2. 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            return Result.fail("账号不存在");
        }

        // 密码校验
        boolean ok = Md5Util.check(dto.getPassword(), user.getPassword(), user.getSalt());
        if (!ok) {
            return Result.fail("密码错误");
        }

        // 3. 生成token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        String token = jwtUtil.generateToken(claims);

        UserLoginVO vo = new UserLoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        
        log.info("用户登录成功, userId: {}, username: {}", user.getId(), username);
        return Result.success(vo);
    }
}