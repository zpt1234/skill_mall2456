package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@TableName("seckill_goods") // 对应数据库表名
public class SeckillGoods {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 秒杀商品名称
    private String seckillGoodsName;

    // 原价（对应origin_price）
    private BigDecimal originPrice;

    // 秒杀价
    private BigDecimal seckillPrice;

    // 库存
    private Integer stockCount;

    // 限购数量
    private Integer limitNum;

    // 商品图片地址
    private String images;

    // 商品描述
    private String goodsDesc;

    // 预热开始时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime preheatTime;

    // 秒杀开始时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime startTime;

    // 秒杀结束时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime endTime;
}