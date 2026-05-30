package com.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.seckill.dto.SeckillGoodsDTO;
import com.seckill.entity.SeckillGoods;
import com.seckill.result.Result;
import com.seckill.service.AdminGoodsService;
import com.seckill.service.SeckillGoodsService;

import com.seckill.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.seckill.util.RedisKeyUtil.SECKILL_GOODS;

@Slf4j
@Service
public class AdminGoodsServiceImpl implements AdminGoodsService {

    @Autowired
    private SeckillGoodsService seckillGoodsService;
    @Autowired
    private RedisTemplate redisTemplate;

    // ===================== 秒杀商品实现 =====================
    @Override
    public Result<List<Map<String, Object>>> listAllSeckillGoods() {
        Set<String> keys = redisTemplate.keys(RedisKeyUtil.SECKILL_GOODS + "*");

        List<SeckillGoods> goodsList = new ArrayList<>();
        if (keys != null && !keys.isEmpty()) {
            List<Object> objects = redisTemplate.opsForValue().multiGet(keys);
            goodsList = objects.stream()
                    .filter(Objects::nonNull)
                    .map(obj -> {
                        if (obj instanceof Map) {
                            return JSON.parseObject(JSON.toJSONString(obj), SeckillGoods.class);
                        }
                        return (SeckillGoods) obj;
                    })
                    .collect(Collectors.toList());
        }

        if (goodsList.isEmpty()) {
            goodsList = seckillGoodsService.list();
            if (goodsList == null || goodsList.isEmpty()) {
                return Result.fail("暂无秒杀商品");
            }
            for (SeckillGoods goods : goodsList) {
                cacheSeckillGoods(goods);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (SeckillGoods goods : goodsList) {
            String json = JSON.toJSONString(goods);
            Map<String, Object> goodsMap = JSON.parseObject(json, new TypeReference<Map<String, Object>>() {});
            
            long preheatCountdown = 0;
            long seckillCountdown = 0;
            String status = "";
            
            if (goods.getPreheatTime() != null && now.isBefore(goods.getPreheatTime())) {
                preheatCountdown = Duration.between(now, goods.getPreheatTime()).getSeconds();
                status = "preheat_pending";
            } else if (goods.getStartTime() != null && now.isBefore(goods.getStartTime())) {
                seckillCountdown = Duration.between(now, goods.getStartTime()).getSeconds();
                status = "seckill_pending";
            } else if (goods.getEndTime() != null && now.isAfter(goods.getEndTime())) {
                status = "ended";
            } else {
                status = "active";
            }
            
            goodsMap.put("preheatCountdown", preheatCountdown);
            goodsMap.put("seckillCountdown", seckillCountdown);
            goodsMap.put("status", status);
            
            resultList.add(goodsMap);
        }

        return Result.success(resultList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addSeckillGoods(SeckillGoodsDTO dto) {
        Result get = getObjectResult(dto);
        if (get != null) return get;
        SeckillGoods seckillGoods = new SeckillGoods();
        BeanUtils.copyProperties(dto,seckillGoods);
        if (seckillGoods.getLimitNum() == null) {
            seckillGoods.setLimitNum(1);
        }
        boolean saveResult = this.seckillGoodsService.save(seckillGoods);
        if (saveResult==false) {
            return Result.fail("新增秒杀商品失败");
        }
        String key= SECKILL_GOODS+seckillGoods.getId();
        long expireSeconds = Duration.between(dto.getStartTime(), dto.getEndTime()).getSeconds();
        redisTemplate.opsForValue().set(key, seckillGoods,
                expireSeconds,
                TimeUnit.SECONDS);

        return Result.success(seckillGoods);
    }

    private static Result getObjectResult(SeckillGoodsDTO dto) {
        if (dto.getSeckillGoodsName() == null || dto.getSeckillGoodsName().trim().isEmpty()) {
            return Result.fail("商品名称不能为空");
        }
        if (dto.getOriginPrice() == null || dto.getOriginPrice().compareTo(BigDecimal.ZERO) < 0) {
            return Result.fail("商品原价不能小于0");
        }
        if (dto.getSeckillPrice() == null || dto.getSeckillPrice().compareTo(BigDecimal.ZERO) < 0) {
            return Result.fail("秒杀价格不能小于0");
        }
        if (dto.getStockCount() == null || dto.getStockCount() < 0) {
            return Result.fail("库存数量不能小于0");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            return Result.fail("秒杀开始/结束时间不能为空");
        }
        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            return Result.fail("秒杀开始时间不能晚于结束时间");
        }
        if (dto.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀结束时间不能早于当前时间");
        }
        return null;
    }

    @Override
    @Transactional
    public Result updateSeckillGoods(Long id, SeckillGoodsDTO dto) {
        // 1. 校验商品是否存在
        SeckillGoods oldGoods = this.seckillGoodsService.getById(id);
        if (oldGoods == null) {
            return Result.fail("修改失败：商品不存在");
        }
        Result objectResult = getObjectResult(dto);
        if (objectResult!=null) return objectResult;
        SeckillGoods updateGoods = new SeckillGoods();
        updateGoods.setId(id);
        BeanUtils.copyProperties(dto, updateGoods);
        boolean updateResult = this.seckillGoodsService.updateById(updateGoods);
        if (!updateResult) {
            return Result.fail("修改商品失败");
        }
        String redisKey = SECKILL_GOODS + id;
        long expireSeconds = Duration.between(updateGoods.getStartTime(), dto.getEndTime()).getSeconds();
        // 直接set覆盖，Redis会自动更新值和过期时间
        redisTemplate.opsForValue().set(
                redisKey,
                updateGoods,
                expireSeconds,
                TimeUnit.SECONDS
        );

        return Result.success(updateGoods);
    }

    @Override
    @Transactional
    public Result deleteSeckillGoods(Long id) {
        // 1. 校验参数
        if (id == null || id <= 0) {
            return Result.fail("商品ID不合法");
        }

        // 2. 校验商品是否存在
        SeckillGoods seckillGoods = this.seckillGoodsService.getById(id);
        if (seckillGoods == null) {
            return Result.fail("删除失败：商品不存在");
        }

        // 3. 第一步：删除数据库数据
        boolean deleteResult = this.seckillGoodsService.removeById(id);
        if (!deleteResult) {
            return Result.fail("删除商品失败");
        }

        // 4. 第二步：删除Redis缓存（和新增的key规则完全一致）
        String redisKey = SECKILL_GOODS + id;
        redisTemplate.delete(redisKey);

        // 5. 返回成功
        return Result.success("删除秒杀商品成功");
    }
    @Override
    public Result<Map<String, Object>> getSeckillGoods(Long id) {
        if (id == null || id <= 0) {
            return Result.fail("商品ID不合法");
        }
        String redisKey = SECKILL_GOODS + id;
        Object obj = redisTemplate.opsForValue().get(redisKey);
        if (obj == null) {
            return Result.fail("秒杀商品不存在或已过期");
        }
        String json = JSON.toJSONString(obj);
        Map<String, Object> goodsMap = JSON.parseObject(json, new TypeReference<Map<String, Object>>() {});
        return Result.success(goodsMap);
    }
    private void cacheSeckillGoods(SeckillGoods goods) {
        // ====================== 1. 基础非空判断 ======================
        if (goods == null || goods.getId() == null) {
            return;
        }

        // ====================== 2. 库存合法性判断 ======================
        // 无库存的秒杀商品，不缓存
        if (goods.getStockCount() == null || goods.getStockCount() <= 0) {
            return;
        }

        // ====================== 3. 秒杀时间合法性判断 ======================
        LocalDateTime endTime = goods.getEndTime();
        LocalDateTime now = LocalDateTime.now();
        // 结束时间为空 或 秒杀已结束 → 不缓存
        if (endTime == null || now.isAfter(endTime)) {
            return;
        }

        // ====================== 4. 计算Redis缓存过期时间 ======================
        // 秒杀剩余时间（毫秒）
        long expireMillis = Duration.between(now, endTime).toMillis();
        // 缓存时长 = 剩余时间 + 1小时缓冲（和你原有逻辑一致）
        long ttl = expireMillis + 3600000;

        // ====================== 5. 仅有效时间才缓存 ======================
        if (ttl > 0) {
            String redisKey = SECKILL_GOODS + goods.getId();
            // 缓存商品对象，设置过期时间
            redisTemplate.opsForValue().set(redisKey, goods, ttl, TimeUnit.MILLISECONDS);
        }
    }
}