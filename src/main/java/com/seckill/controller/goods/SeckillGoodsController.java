package com.seckill.controller.goods;

import com.seckill.entity.SeckillGoods;
import com.seckill.result.Result;
import com.seckill.service.AdminGoodsService;
import com.seckill.service.SeckillGoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goods")
public class SeckillGoodsController {

    @Autowired
    private SeckillGoodsService seckillGoodsService;
    @Autowired
    private AdminGoodsService adminGoodsService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/seckill/list")
    public Result<List<Map<String, Object>>> getSeckillGoodsList() {
        return adminGoodsService.listAllSeckillGoods();
    }

    @GetMapping("/seckill/detail/{id}")
    public Result<Map<String, Object>> getGoodsDetail(@PathVariable Long id) {
        SeckillGoods goods = seckillGoodsService.getById(id);
        if (goods == null) {
            return Result.fail("商品不存在或已下架");
        }

        String redisStockKey = "seckill:goods:stock:" + id;
        Object stockObj = redisTemplate.opsForValue().get(redisStockKey);
        if (stockObj != null) {
            try {
                int realTimeStock = Integer.parseInt(stockObj.toString());
                goods.setStockCount(realTimeStock);
            } catch (NumberFormatException e) {
                // 如果转换失败，保持数据库库存
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("goods", goods);
        
        LocalDateTime now = LocalDateTime.now();
        long preheatCountdown = 0;
        long seckillCountdown = 0;
        String status = "";
        
        // 判断预热状态
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
        
        result.put("preheatCountdown", preheatCountdown);
        result.put("seckillCountdown", seckillCountdown);
        result.put("status", status);
        
        return Result.success(result);
    }
}