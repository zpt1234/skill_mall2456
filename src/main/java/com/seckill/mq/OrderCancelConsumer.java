package com.seckill.mq;

import com.rabbitmq.client.Channel;
import com.seckill.config.RabbitMQConfig;
import com.seckill.entity.Order;
import com.seckill.mapper.OrderMapper;
import com.seckill.mapper.SeckillGoodsMapper;
import com.seckill.util.RedisKeyUtil;
import com.seckill.vo.OrderCancelMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Slf4j
@Component
public class OrderCancelConsumer {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final Integer UN_PAY = 0;
    private static final Integer EXPIRED = 3;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCEL_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderCancel(OrderCancelMessage message, Channel channel, Message mqMessage) throws IOException {
        long deliveryTag = mqMessage.getMessageProperties().getDeliveryTag();
        Long orderId = message.getOrderId();
        String orderNo = message.getOrderNo();
        Long goodsId = message.getGoodsId();
        Long userId = message.getUserId();

        log.info("========== 订单超时取消消费者触发 ========== orderNo={}|orderId={}", orderNo, orderId);

        try {
            Order order = orderMapper.selectById(orderId);

            if (order == null) {
                log.warn("订单不存在，跳过取消|orderId={}", orderId);
                safeAck(channel, deliveryTag);
                return;
            }

            if (order.getOrderStatus() != UN_PAY) {
                log.info("订单已支付或已取消，无需处理|orderNo={}|status={}", orderNo, order.getOrderStatus());
                safeAck(channel, deliveryTag);
                return;
            }

            order.setOrderStatus(EXPIRED);
            orderMapper.updateById(order);

            if (goodsId != null) {
                int affected = seckillGoodsMapper.increaseStock(goodsId, 1);
                if (affected > 0) {
                    log.info("库存回退成功|goodsId={}|orderNo={}", goodsId, orderNo);
                } else {
                    log.error("库存回退失败|goodsId={}|orderNo={}", goodsId, orderNo);
                }
                
                String stockKey = RedisKeyUtil.SECKILL_STOCK + goodsId;
                redisTemplate.opsForValue().increment(stockKey, 1);
                log.info("订单超时Redis库存回退成功|goodsId={}|stockKey={}", goodsId, stockKey);
                
                if (userId != null) {
                    String userKey = RedisKeyUtil.SECKILL_USER + goodsId + ":" + userId;
                    redisTemplate.delete(userKey);
                    log.info("订单超时删除防重标记|goodsId={}|userId={}", goodsId, userId);
                    
                    String orderUniqKey = RedisKeyUtil.SECKILL_ORDER_UNIQ + goodsId + ":" + userId;
                    redisTemplate.delete(orderUniqKey);
                    log.info("订单超时删除幂等标记|goodsId={}|userId={}", goodsId, userId);
                }
            }

            log.info("订单超时取消成功|orderNo={}|orderId={}|userId={}", orderNo, orderId, order.getUserId());
            safeAck(channel, deliveryTag);

        } catch (Exception e) {
            log.error("订单超时取消失败|orderNo={}|orderId={}", orderNo, orderId, e);
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
}
