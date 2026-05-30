package com.seckill.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

    @Configuration
    public class RedisConfig {

        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(factory);

            // Key 明文序列化
            template.setKeySerializer(new StringRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());

            // Value JSON 序列化：支持 Java 8 时间，启用多态类型
            ObjectMapper mapper = new ObjectMapper();
            mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
            mapper.registerModule(new JavaTimeModule());

            // 【核心修复】启用默认类型支持，使 Jackson 在 JSON 中携带 @class 信息
            mapper.activateDefaultTyping(
                    BasicPolymorphicTypeValidator.builder()
                            .allowIfBaseType(Object.class)
                            .build(),
                    ObjectMapper.DefaultTyping.NON_FINAL
            );

            GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(mapper);
            template.setValueSerializer(jsonSerializer);
            template.setHashValueSerializer(jsonSerializer);

            template.afterPropertiesSet();
            return template;
        }
}