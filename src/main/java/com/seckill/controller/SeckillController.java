package com.seckill.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.seckill.result.Result;
import com.seckill.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import javax.servlet.http.HttpServletRequest;

/**
 * 秒杀接口控制器
 * 面试考点：Sentinel QPS限流 + 热点参数IP限流
 */
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    /**
     * 立即秒杀接口
     */
    @PostMapping("/do/{goodsId}")
    @SentinelResource(value = "doSeckill", blockHandler = "doSeckillBlockHandler")
    public Result<String> doSeckill(@PathVariable Long goodsId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        String ip = getClientIP(request);
        
        return seckillService.doSeckill(goodsId, userId, ip);
    }

    /**
     * Sentinel QPS限流兜底方法
     */
    public Result<String> doSeckillBlockHandler(Long goodsId, HttpServletRequest request, BlockException ex) {
        return Result.fail("系统繁忙，请稍后再试");
    }

    /**
     * 从 LoginInterceptor 解析当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj == null) {
            return null;
        }
        try {
            return Long.valueOf(userIdObj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip.split(",")[0] : "unknown";
    }
}