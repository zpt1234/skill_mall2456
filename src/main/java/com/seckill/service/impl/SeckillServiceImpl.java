package com.seckill.service.impl;

import com.seckill.config.RabbitMQConfig;
import com.seckill.entity.SeckillGoods;
import com.seckill.mapper.SeckillGoodsMapper;
import com.seckill.result.Result;
import com.seckill.service.SeckillService;
import com.seckill.util.RedisKeyUtil;
import com.seckill.vo.SeckillMessage;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    /**
     * Lua脚本：原子性检查并扣减库存
     * 返回 -2：库存Key未初始化；返回 -1：库存已售罄；返回 >=0：扣减后的剩余库存
     */
    private static final String STOCK_DEDUCT_LUA =
            "if (redis.call('exists', KEYS[1]) == 0) then " +
                    "    return -2; " +
                    "end; " +
                    "local stock = tonumber(redis.call('get', KEYS[1])); " +
                    "if (stock == nil) then " +
                    "    return -2; " +
                    "end; " +
                    "if (stock <= 0) then " +
                    "    return -1; " +
                    "end; " +
                    "redis.call('decr', KEYS[1]); " +
                    "return stock - 1;";

    @Override
    public Result<String> doSeckill(Long goodsId, Long userId, String ip) {
        try {
            // ==================== 1. 秒杀时间校验 ====================
            SeckillGoods goods = getSeckillGoodsFromRedis(goodsId);
            if (goods == null) {
                goods = seckillGoodsMapper.selectById(goodsId);
                if (goods == null) {
                    return Result.fail("商品信息不存在");
                }
                cacheSeckillGoods(goods);
            }

            LocalDateTime now = LocalDateTime.now();
            // 秒杀开始/结束时间
            LocalDateTime startTime = goods.getStartTime();
            LocalDateTime endTime = goods.getEndTime();

            // 时间比较：LocalDateTime 直接用 isBefore / isAfter 最优雅
            if (now.isBefore(startTime)) {
                return Result.fail("秒杀活动尚未开始");
            }
            if (now.isAfter(endTime)) {
                return Result.fail("秒杀活动已结束");
            }

            // ==================== 2. 用户防重秒杀 ====================
            String userKey = RedisKeyUtil.SECKILL_USER + goodsId + ":" + userId;
            long expireMillis = Duration.between(now, endTime).toMillis();
            // 兜底：防止时间为负
            if (expireMillis <= 0) {
                return Result.fail("秒杀活动已结束");
            }
            long userTtl = expireMillis + 3600000;
            Boolean isFirst = redisTemplate.opsForValue()
                    .setIfAbsent(userKey, "1", userTtl, TimeUnit.MILLISECONDS);

            if (Boolean.FALSE.equals(isFirst)) {
                return Result.fail("您已参与过该场秒杀，请勿重复下单");
            }

            // ==================== 3. Redis预减库存 + 分布式防超卖 ====================
            String stockKey = RedisKeyUtil.SECKILL_STOCK + goodsId;
            String lockKey = RedisKeyUtil.SECKILL_STOCK_LOCK + goodsId;
            RLock lock = redissonClient.getLock(lockKey);
            boolean isLocked = false;

            try {
                isLocked = lock.tryLock(3, 10, TimeUnit.SECONDS);
                if (!isLocked) {
                    redisTemplate.delete(userKey);
                    return Result.fail("系统繁忙，请稍后再试");
                }

                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                redisScript.setScriptText(STOCK_DEDUCT_LUA);
                redisScript.setResultType(Long.class);

                Long stockResult = redisTemplate.execute(redisScript, Collections.singletonList(stockKey));

                if (stockResult == null || stockResult == -2) {
                    initStockFromDB(goods);
                    stockResult = redisTemplate.execute(redisScript, Collections.singletonList(stockKey));
                }

                if (stockResult == null || stockResult < 0) {
                    redisTemplate.delete(userKey);
                    return Result.fail("商品已被抢完");
                }
                SeckillGoods seckillGoods = getSeckillGoodsFromRedis(goodsId);
                seckillGoods.setStockCount(stockResult.intValue()); // 设置为最新剩余库存
                cacheSeckillGoods(seckillGoods);

                // ==================== 4. 请求异步削峰（RabbitMQ） ====================
                SeckillMessage message = new SeckillMessage();
                message.setUserId(userId);
                message.setGoodsId(goodsId);
                message.setGoodsName(goods.getSeckillGoodsName());
                message.setSeckillPrice(goods.getSeckillPrice());
                message.setOrderUniqKey(RedisKeyUtil.SECKILL_ORDER_UNIQ + goodsId + ":" + userId);

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.ORDER_CREATE_EXCHANGE,
                        RabbitMQConfig.ORDER_CREATE_ROUTING_KEY,
                        message
                );

                log.info("秒杀成功|userId={}|goodsId={}|remainStock={}", userId, goodsId, stockResult);
                return Result.success("秒杀成功，订单处理中...");

            } finally {
                if (isLocked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.fail("系统中断");
        } catch (Exception e) {
            log.error("秒杀异常|userId={}|goodsId={}", userId, goodsId, e);
            return Result.fail("系统异常，请稍后再试");
        }
    }

    private SeckillGoods getSeckillGoodsFromRedis(Long goodsId) {
        String key = "seckill:goods:" + goodsId;
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) {
            return null;
        }
        // 新数据：有 @class，直接就是 SeckillGoods
        if (obj instanceof SeckillGoods) {
            return (SeckillGoods) obj;
        }
        // 旧数据：被反序列化成了 LinkedHashMap，手动转换
        if (obj instanceof java.util.LinkedHashMap) {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper.convertValue(obj, SeckillGoods.class);
        }
        return null;
    }

    private void cacheSeckillGoods(SeckillGoods goods) {
        String key = "seckill:goods:" + goods.getId();
        // 计算Redis缓存有效期：秒杀剩余时间 + 1小时缓冲(3600000ms)
        long expireMillis = Duration.between(LocalDateTime.now(), goods.getEndTime()).toMillis();
        long ttl = expireMillis + 3600000;
        // 只有剩余时间>0时才缓存，避免过期时间为负数
        if (ttl > 0) {
            redisTemplate.opsForValue().set(key, goods, ttl, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 从数据库初始化秒杀库存到Redis
     */
    private void initStockFromDB(SeckillGoods goods) {
        String stockKey = RedisKeyUtil.SECKILL_STOCK + goods.getId();
        // 计算Redis库存有效期：和商品缓存保持一致
        long expireMillis = Duration.between(LocalDateTime.now(), goods.getEndTime()).toMillis();
        long ttl = expireMillis + 3600000;
        // 原子初始化库存
        if (ttl > 0) {
            redisTemplate.opsForValue().setIfAbsent(stockKey, goods.getStockCount(), ttl, TimeUnit.MILLISECONDS);
        }
    }
}