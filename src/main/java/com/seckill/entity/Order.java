package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_info")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long goodsId;
    private Long seckillGoodsId;
    private String goodsName;
    private BigDecimal goodsPrice;
    private String orderNo;
    private BigDecimal orderPrice;
    private Integer orderStatus;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime expireTime;
}