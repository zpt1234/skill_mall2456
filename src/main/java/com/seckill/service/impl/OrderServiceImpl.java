package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.entity.Order;
import com.seckill.entity.SeckillGoods;
import com.seckill.mapper.OrderMapper;
import com.seckill.mapper.SeckillGoodsMapper;
import com.seckill.result.Result;
import com.seckill.service.OrderService;
import com.seckill.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final Integer UN_PAY = 0;
    private static final Integer PAYED = 1;
    private static final Integer CANCEL = 2;
    private static final Integer EXPIRED = 3;

    @Override
    public Result<List<Order>> getOrderList(Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime);
        List<Order> orders = orderMapper.selectList(wrapper);
        return Result.success(orders);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> payOrder(String orderNo, Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId);
        Order order = orderMapper.selectOne(wrapper);

        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (order.getOrderStatus() != 0) {
            return Result.fail("订单状态异常，无法支付");
        }

        order.setOrderStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("订单支付成功|orderNo={}", orderNo);
        return Result.success("支付成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> cancelOrder(String orderNo, Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId);
        Order order = orderMapper.selectOne(wrapper);

        if (order == null) {
            return Result.fail("订单不存在");
        }
        
        if (!UN_PAY.equals(order.getOrderStatus())) {
            return Result.fail("订单状态异常，无法取消");
        }

        order.setOrderStatus(CANCEL);
        order.setExpireTime(LocalDateTime.now());
        orderMapper.updateById(order);

        Long goodsId = order.getGoodsId();
        if (goodsId != null) {
            int affected = seckillGoodsMapper.increaseStock(goodsId, 1);
            if (affected > 0) {
                log.info("取消订单库存回退成功|goodsId={}", goodsId);
            } else {
                log.error("取消订单库存回退失败|goodsId={}", goodsId);
            }
            
            String stockKey = RedisKeyUtil.SECKILL_STOCK + goodsId;
            redisTemplate.opsForValue().increment(stockKey, 1);
            
            SeckillGoods goods = seckillGoodsMapper.selectById(goodsId);
            if (goods != null) {
                String goodsKey = "seckill:goods:" + goodsId;
                Object goodsObj = redisTemplate.opsForValue().get(goodsKey);
                if (goodsObj != null) {
                    goods.setStockCount(redisTemplate.opsForValue().get(stockKey) != null 
                            ? Integer.parseInt(redisTemplate.opsForValue().get(stockKey).toString()) 
                            : goods.getStockCount());
                    redisTemplate.opsForValue().set(goodsKey, goods);
                }
            }
            
            String userKey = RedisKeyUtil.SECKILL_USER + goodsId + ":" + userId;
            redisTemplate.delete(userKey);
            
            String orderUniqKey = RedisKeyUtil.SECKILL_ORDER_UNIQ + goodsId + ":" + userId;
            redisTemplate.delete(orderUniqKey);
        }

        log.info("订单取消成功|orderNo={}", orderNo);
        return Result.success("订单已取消");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteOrder(String orderNo, Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId);
        Order order = orderMapper.selectOne(wrapper);

        if (order == null) {
            return Result.fail("订单不存在");
        }
        
        if (!CANCEL.equals(order.getOrderStatus()) && !EXPIRED.equals(order.getOrderStatus())) {
            return Result.fail("只能删除已取消或已过期的订单");
        }

        int deleted = orderMapper.deleteById(order.getId());
        if (deleted > 0) {
            log.info("订单删除成功|orderNo={}", orderNo);
            return Result.success("订单已删除");
        } else {
            log.error("删除订单失败|orderNo={}", orderNo);
            return Result.fail("删除订单失败");
        }
    }
}