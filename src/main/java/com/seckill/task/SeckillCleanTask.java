package com.seckill.task;

import com.seckill.entity.SeckillGoods;
import com.seckill.mapper.OrderMapper;
import com.seckill.service.SeckillGoodsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务：秒杀结束后，自动删除Redis + 数据库商品
 */
@Slf4j
@Component
public class SeckillCleanTask {

    @Autowired
    private SeckillGoodsService seckillGoodsService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private OrderMapper orderMapper;

    // 每分钟执行一次：清理已结束的秒杀商品
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void cleanExpiredSeckillGoods() {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillGoods> expiredList = seckillGoodsService.lambdaQuery()
                .lt(SeckillGoods::getEndTime, now)
                .list();

        if (expiredList != null && !expiredList.isEmpty()) {
            log.info("开始清理过期秒杀商品，数量: {}", expiredList.size());
            for (SeckillGoods goods : expiredList) {
                Long id = goods.getId();
                log.info("清理过期的商品订单：{}",id);
                orderMapper.deleteById(id);
                redisTemplate.delete("seckill:goods:" + id);
                redisTemplate.delete("seckill:stock:" + id);
                seckillGoodsService.removeById(id);
                log.info("已清理过期秒杀商品，ID：{}", id);
            }
        }
    }
}