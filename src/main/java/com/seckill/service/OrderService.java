package com.seckill.service;

import com.seckill.entity.Order;
import com.seckill.result.Result;

import java.util.List;

public interface OrderService {
    Result<List<Order>> getOrderList(Long userId);
    Result<String> payOrder(String orderNo, Long userId);
    Result<String> cancelOrder(String orderNo, Long userId);
    Result<String> deleteOrder(String orderNo, Long userId);
}