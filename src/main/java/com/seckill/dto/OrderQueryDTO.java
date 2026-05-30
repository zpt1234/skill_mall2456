package com.seckill.dto;

import lombok.Data;
//订单分页查询
@Data
public class OrderQueryDTO {
    private String orderId;       // 订单ID
    private String userId;        // 用户ID
    private Integer orderStatus;  // 订单状态
    private Long pageNum;         // 页码
    private Long pageSize;        // 每页条数
}