package com.seckill.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderCancelMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long goodsId;
    private Integer stockCount;
}
