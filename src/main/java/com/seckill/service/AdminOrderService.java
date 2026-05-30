package com.seckill.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.dto.OrderQueryDTO;
import com.seckill.entity.Order;


public interface AdminOrderService {
    // 后台分页查询订单
    Page<Order> getAdminOrderPage(OrderQueryDTO dto);

    // 根据ID查询订单详情
    Order getOrderById(Long orderId);

    // 管理员取消订单
    boolean adminCancelOrder(Long orderId);

    // 修改订单状态
    boolean updateOrderStatus(Long orderId, Integer status);
}
