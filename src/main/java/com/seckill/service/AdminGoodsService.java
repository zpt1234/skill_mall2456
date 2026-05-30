package com.seckill.service;

import com.seckill.dto.SeckillGoodsDTO;
import com.seckill.result.Result;

import java.util.List;
import java.util.Map;

public interface AdminGoodsService {
    // ===================== 秒杀商品接口 =====================
    Result<List<Map<String, Object>>> listAllSeckillGoods();

    Result addSeckillGoods(SeckillGoodsDTO dto);

    Result updateSeckillGoods(Long id, SeckillGoodsDTO dto);

    Result deleteSeckillGoods(Long id);

    Result<Map<String, Object>> getSeckillGoods(Long id);
}