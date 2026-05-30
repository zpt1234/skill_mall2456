package com.seckill.util;

public class RedisKeyUtil {
    private static final String PREFIX = "seckill:";
    public static final String SECKILL_STOCK = PREFIX + "stock:";

    //  用户秒杀防重键
    public static final String SECKILL_USER = PREFIX + "user:";
    public static final String SECKILL_GOODS = PREFIX+ "goods:";

    //  订单幂等性键
    public static final String SECKILL_ORDER_UNIQ = PREFIX + "order:uniq:";
    //  库存操作分布式锁键
    public static final String SECKILL_STOCK_LOCK = PREFIX + "lock:stock:";
    
    // AI对话记忆功能
    public static final String AI_CHAT_HISTORY = PREFIX + "ai:chat:";

    /**
     * AI聊天Query缓存前缀
     */
    public static final String AI_CHAT_CACHE = "ai:chat:cache:";

    /**
     * AI商品上下文前缀
     */
    public static final String AI_PRODUCT_CONTEXT = "ai:product:context:";
}