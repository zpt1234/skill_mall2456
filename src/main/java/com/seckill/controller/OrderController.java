package com.seckill.controller;

import com.seckill.result.Result;
import com.seckill.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/list")
    public Result<?> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        return orderService.getOrderList(userId);
    }

    @PostMapping("/pay")
    public Result<String> pay(@RequestBody Map<String, String> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        String orderNo = params.get("orderNo");
        return orderService.payOrder(orderNo, userId);
    }

    @PostMapping("/cancel")
    public Result<String> cancel(@RequestBody Map<String, String> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        String orderNo = params.get("orderNo");
        return orderService.cancelOrder(orderNo, userId);
    }
    
    @PostMapping("/delete")
    public Result<String> delete(@RequestBody Map<String, String> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        String orderNo = params.get("orderNo");
        return orderService.deleteOrder(orderNo, userId);
    }
}