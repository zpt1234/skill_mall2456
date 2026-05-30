package com.seckill.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // 订单创建交换机/队列
    public static final String ORDER_CREATE_EXCHANGE = "order.create.exchange";
    public static final String ORDER_CREATE_QUEUE = "order.create.queue";
    public static final String ORDER_CREATE_ROUTING_KEY = "order.create";

    // 延迟取消订单交换机/队列
    public static final String ORDER_CANCEL_EXCHANGE = "order.cancel.exchange";
    public static final String ORDER_CANCEL_QUEUE = "order.cancel.queue";
    public static final String ORDER_CANCEL_ROUTING_KEY = "order.cancel";

    /**
     * 配置JSON消息转换器（解决反序列化安全问题）
     */
    @Bean
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        return converter;
    }

    /**
     * 配置RabbitTemplate使用JSON消息转换器
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    // --------------------- 1. 订单创建队列 ---------------------
    @Bean
    public DirectExchange orderCreateExchange() {
        return new DirectExchange(ORDER_CREATE_EXCHANGE);
    }
    @Bean
    public Queue orderCreateQueue() {
        return new Queue(ORDER_CREATE_QUEUE, true);
    }
    @Bean
    public Binding orderCreateBinding() {
        return BindingBuilder.bind(orderCreateQueue())
                .to(orderCreateExchange())
                .with(ORDER_CREATE_ROUTING_KEY);
    }

    // --------------------- 2. 延迟订单取消队列 ---------------------
    @Bean
    public CustomExchange orderCancelExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(ORDER_CANCEL_EXCHANGE, "x-delayed-message", true, false, args);
    }
    @Bean
    public Queue orderCancelQueue() {
        return new Queue(ORDER_CANCEL_QUEUE, true);
    }
    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue())
                .to(orderCancelExchange())
                .with(ORDER_CANCEL_ROUTING_KEY)
                .noargs();
    }
}