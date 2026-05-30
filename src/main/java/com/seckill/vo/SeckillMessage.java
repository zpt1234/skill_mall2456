package com.seckill.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SeckillMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long goodsId;
    private String goodsName;
    private BigDecimal seckillPrice;
    /** 订单幂等性Key：seckill:order:uniq:{goodsId}:{userId} */
    private String orderUniqKey;
}