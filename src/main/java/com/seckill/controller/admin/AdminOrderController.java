package com.seckill.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.seckill.dto.OrderQueryDTO;
import com.seckill.dto.OrderStatusDTO;
import com.seckill.entity.Order;
import com.seckill.result.Result;
import com.seckill.service.AdminOrderService;

import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

@RestController
@RequestMapping("/api/order/admin")
public class AdminOrderController {

    @Resource
    private AdminOrderService adminOrderService;

    /**
     * 1. 分页查询订单列表（前端调用：/order/admin/page GET）
     */
    @GetMapping("/page")
    public Result<Page<Order>> orderPage(OrderQueryDTO dto) {
        Page<Order> page = adminOrderService.getAdminOrderPage(dto);
        return Result.success(page);
    }

    /**
     * 2. 查询订单详情（前端调用：/order/admin/detail/{orderId} GET）
     */
    @GetMapping("/detail/{orderId}")
    public Result<Order> getDetail(@PathVariable Long orderId) {
        Order order = adminOrderService.getOrderById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 3. 管理员取消订单（前端调用：/order/admin/cancel/{orderId} POST）
     */
    @PostMapping("/cancel/{orderId}")
    public Result<Void> cancelOrder(@PathVariable Long orderId) {
        boolean ok = adminOrderService.adminCancelOrder(orderId);
        if (!ok) {
            return Result.fail("仅可取消未支付订单");
        }
        return Result.success();
    }

    /**
     * 4. 修改订单状态（前端调用：/order/admin/updateStatus POST）
     */
    @PostMapping("/updateStatus")
    public Result<Void> updateStatus(@RequestBody OrderStatusDTO dto) {
        boolean ok = adminOrderService.updateOrderStatus(dto.getOrderId(), dto.getStatus());
        if (!ok) {
            return Result.fail("订单状态修改失败");
        }
        return Result.success();
    }
}