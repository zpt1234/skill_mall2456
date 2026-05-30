package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.dto.OrderQueryDTO;


import com.seckill.entity.Order;
import com.seckill.mapper.AdminOrderMapper;
import com.seckill.service.AdminOrderService;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class AdminOrderServiceIml implements AdminOrderService {
    @Resource
    private AdminOrderMapper adminOrderMapper;
    private static final Integer UN_PAY = 0;    // 未支付
    private static final Integer PAYED = 1;     // 已支付
    private static final Integer CANCEL = 2;    // 已取消
    private static final Integer EXPIRED = 3;   // 已过期


    @Override
    public Page<Order> getAdminOrderPage(OrderQueryDTO dto) {
        Page<Order> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();

        // 订单ID查询
        if (StringUtils.hasText(dto.getOrderId())) {
            wrapper.like(Order::getId, dto.getOrderId());
        }
        // 用户ID查询
        if (StringUtils.hasText(dto.getUserId())) {
            wrapper.like(Order::getUserId, dto.getUserId());
        }
        // 订单状态查询
        if (dto.getOrderStatus() != null) {
            wrapper.eq(Order::getOrderStatus, dto.getOrderStatus());
        }
        // 按创建时间倒序
        wrapper.orderByDesc(Order::getCreateTime);
        return adminOrderMapper.selectPage(page, wrapper);
    }

    @Override
    public Order getOrderById(Long orderId) {
        return adminOrderMapper.selectById(orderId);
    }

    @Override
    public boolean adminCancelOrder(Long orderId) {
        Order order = adminOrderMapper.selectById(orderId);
        // 订单不存在
        if (order == null) {
            return false;
        }
        // 仅允许取消【未支付】订单
        if (!UN_PAY.equals(order.getOrderStatus())) {
            return false;
        }
        // 更新为已取消
        order.setOrderStatus(CANCEL);
        return adminOrderMapper.updateById(order) > 0;
    }

    @Override
    public boolean updateOrderStatus(Long orderId, Integer status) {
        Order order = adminOrderMapper.selectById(orderId);
        if (order == null) {
            return false;
        }
        // 业务规则：未支付 → 已支付；已支付 可修改状态
        Integer currentStatus = order.getOrderStatus();
        if (currentStatus.equals(UN_PAY) && status.equals(PAYED)) {
            order.setOrderStatus(status);
            order.setPayTime(LocalDateTime.now());
            return adminOrderMapper.updateById(order) > 0;
        }
        return false;
    }
}
