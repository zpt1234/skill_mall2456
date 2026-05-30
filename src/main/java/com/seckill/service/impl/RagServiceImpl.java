package com.seckill.service.impl;

import com.seckill.service.RagService;
import com.seckill.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * RAG检索服务实现（简化版）
 * 当前基于Redis关键词匹配，后续可升级为向量检索
 */
@Slf4j
@Service
public class RagServiceImpl implements RagService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 初始化商品FAQ到Redis（演示用，实际应从数据库加载）
     * 项目启动时执行
     */
    @PostConstruct
    public void initProductFaq() {
        // 示例：为商品ID=1设置FAQ
        Map<String, String> faqMap = new HashMap<>();
        faqMap.put("1", "商品名称：iPhone 15 Pro\n" +
                "秒杀价格：6999元（原价7999元）\n" +
                "库存：100件\n" +
                "限购：每人限购1件\n" +
                "发货时间：付款后48小时内\n" +
                "售后服务：支持7天无理由退货，15天换货\n" +
                "活动规则：2024-06-18 10:00开抢，需提前预约");

        faqMap.put("2", "商品名称：小米14\n" +
                "秒杀价格：2999元（原价3499元）\n" +
                "库存：200件\n" +
                "限购：每人限购2件\n" +
                "发货时间：付款后24小时内\n" +
                "售后服务：支持7天无理由退货\n" +
                "活动规则：2024-06-18 14:00开抢");

        for (Map.Entry<String, String> entry : faqMap.entrySet()) {
            String key = RedisKeyUtil.AI_PRODUCT_CONTEXT + entry.getKey();
            stringRedisTemplate.opsForValue().set(key, entry.getValue());
            log.info("初始化商品FAQ|productId={}", entry.getKey());
        }
    }

    @Override
    public String retrieve(Long productId, String query) {
        if (productId == null) {
            return null;
        }

        try {
            String key = RedisKeyUtil.AI_PRODUCT_CONTEXT + productId;
            String context = stringRedisTemplate.opsForValue().get(key);

            if (context != null) {
                log.info("RAG检索命中|productId={}|query={}", productId, query);
            }

            return context;
        } catch (Exception e) {
            log.error("RAG检索失败|productId={}|query={}", productId, query, e);
            return null;
        }
    }
}