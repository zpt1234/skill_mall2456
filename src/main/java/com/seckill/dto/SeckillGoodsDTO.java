package com.seckill.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品DTO
 */
@Data
public class SeckillGoodsDTO {

    /**
     * 秒杀商品名称
     */
    private String seckillGoodsName;

    /**
     * 商品原价
     */
    private BigDecimal originPrice;

    /**
     * 秒杀活动价
     */
    private BigDecimal seckillPrice;

    /**
     * 秒杀总库存
     */
    private Integer stockCount;

    /**
     * 单人限购数量
     */
    private Integer limitNum;

    /**
     * 商品图片
     */
    private String images;

    /**
     * 商品详情描述
     */
    private String goodsDesc;

    /**
     * 预热开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime preheatTime;

    /**
     * 秒杀开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 秒杀结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime endTime;
}
