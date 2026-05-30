package com.seckill.dto;


import lombok.Data;

@Data
public class OrderStatusDTO {
    private Long orderId;    // 订单ID
    private Integer status;  // 目标状态
}