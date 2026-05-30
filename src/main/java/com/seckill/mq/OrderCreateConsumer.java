package com.seckill.mq;

import com.rabbitmq.client.Channel;
import com.seckill.config.RabbitMQConfig;
import com.seckill.entity.Order;
import com.seckill.mapper.OrderMapper;
import com.seckill.mapper.SeckillGoodsMapper;
import com.seckill.util.RedisKeyUtil;
import com.seckill.vo.SeckillMessage;
import com.seckill.vo.OrderCancelMessage;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OrderCreateConsumer {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATE_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleOrderCreate(SeckillMessage message, Channel channel, Message mqMessage) throws IOException {
        long deliveryTag = mqMessage.getMessageProperties().getDeliveryTag();
        Long userId = message.getUserId();
        Long goodsId = message.getGoodsId();

        log.info("========== MQ消费者收到消息 ========== userId={}|goodsId={}|goodsName={}", 
                userId, goodsId, message.getGoodsName());

        try {
            String uniqKey = message.getOrderUniqKey();
            Boolean isNew = redisTemplate.opsForValue()
                    .setIfAbsent(uniqKey, "processing", 10, TimeUnit.MINUTES);

            if (Boolean.FALSE.equals(isNew)) {
                String status = (String) redisTemplate.opsForValue().get(uniqKey);
                if ("success".equals(status)) {
                    log.warn("订单重复消费，已处理成功|userId={}|goodsId={}", userId, goodsId);
                    safeAck(channel, deliveryTag);
                    return;
                }
                log.warn("订单正在处理中，稍后重试|userId={}|goodsId={}", userId, goodsId);
                safeNack(channel, deliveryTag, true);
                return;
            }

            String orderLockKey = RedisKeyUtil.SECKILL_STOCK_LOCK + goodsId + ":order";
            RLock lock = redissonClient.getLock(orderLockKey);
            boolean isLocked = lock.tryLock(5, 30, TimeUnit.SECONDS);

            if (!isLocked) {
                log.error("获取订单锁失败|userId={}|goodsId={}", userId, goodsId);
                safeNack(channel, deliveryTag, true);
                return;
            }

            try {
                Order order = new Order();
                order.setOrderNo(generateOrderNo(userId, goodsId));
                order.setUserId(userId);
                order.setGoodsId(goodsId);
                order.setSeckillGoodsId(goodsId);
                order.setGoodsName(message.getGoodsName());
                order.setGoodsPrice(message.getSeckillPrice());
                order.setOrderPrice(message.getSeckillPrice());
                order.setOrderStatus(0);
                order.setCreateTime(LocalDateTime.now());
                order.setExpireTime(LocalDateTime.now().plusMinutes(15));

                orderMapper.insert(order);

                int affected = seckillGoodsMapper.decreaseStock(goodsId);
                if (affected <= 0) {
                    throw new RuntimeException("数据库库存不足，回滚订单");
                }

                redisTemplate.opsForValue().set(uniqKey, "success", 24, TimeUnit.HOURS);
                log.info("订单创建成功|orderNo={}|userId={}|goodsId={}", order.getOrderNo(), userId, goodsId);

                OrderCancelMessage cancelMessage = new OrderCancelMessage();
                cancelMessage.setOrderId(order.getId());
                cancelMessage.setOrderNo(order.getOrderNo());
                cancelMessage.setUserId(userId);
                cancelMessage.setGoodsId(goodsId);
                cancelMessage.setStockCount(1);

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.ORDER_CANCEL_EXCHANGE,
                        RabbitMQConfig.ORDER_CANCEL_ROUTING_KEY,
                        cancelMessage,
                        msg -> {
                            msg.getMessageProperties().setDelay(15 * 60 * 1000);
                            return msg;
                        }
                );
                log.info("订单取消延迟消息已发送|orderNo={}|delay=15min", order.getOrderNo());

                safeAck(channel, deliveryTag);

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (Exception e) {
            log.error("订单创建失败|message={}", message, e);
            safeNack(channel, deliveryTag, false);
        }
    }

    private void safeAck(Channel channel, long deliveryTag) {
        try {
            if (channel != null && channel.isOpen()) {
                channel.basicAck(deliveryTag, false);
            }
        } catch (Exception e) {
            log.error("ACK失败|deliveryTag={}", deliveryTag, e);
        }
    }

    private void safeNack(Channel channel, long deliveryTag, boolean requeue) {
        try {
            if (channel != null && channel.isOpen()) {
                channel.basicNack(deliveryTag, false, requeue);
                }
        } catch (Exception e) {
            log.error("NACK失败|deliveryTag={}|requeue={}", deliveryTag, requeue, e);
        }
    }

    private String generateOrderNo(Long userId, Long goodsId) {
        return "SK" + System.currentTimeMillis() + userId + goodsId;
    }
}