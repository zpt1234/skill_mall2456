package com.seckill;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;

@Slf4j
@MapperScan("com.seckill.mapper")
@EnableDiscoveryClient
@SpringBootApplication
@EnableScheduling // 🔥 开启定时任务（自动清理过期秒杀商品）
public class SeckillMall1Application {

    public static void main(String[] args) {
        log.info("=== SeckillMall1 服务启动中 ===");
        SpringApplication.run(SeckillMall1Application.class, args);
        log.info("=== SeckillMall1 服务启动成功 ===");
    }

    @PostConstruct
    public void init() {
        log.info("正在初始化服务组件...");
    }

}