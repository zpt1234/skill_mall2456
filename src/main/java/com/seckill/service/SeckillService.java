package com.seckill.service;

import com.seckill.result.Result;

public interface SeckillService {
    /**
     * 执行秒杀
     * @param goodsId 商品ID
     * @param userId  用户ID
     * @param ip      用户IP（用于日志和风控）
     */
    Result<String> doSeckill(Long goodsId, Long userId, String ip);
}