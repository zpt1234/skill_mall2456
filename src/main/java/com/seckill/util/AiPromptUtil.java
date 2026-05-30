package com.seckill.util;

/**
 * AI Prompt模板工具类
 */
public class AiPromptUtil {

    /**
     * 基础System Prompt
     */
    public static String buildBaseSystemPrompt() {
        return "你是秒杀商城的智能客服助手。你的职责：\n" +
                "1. 帮助用户解答订单相关问题（查询、支付、取消、退款）\n" +
                "2. 解答秒杀活动规则（时间、库存、限购）\n" +
                "3. 处理常见技术问题（登录、页面加载）\n" +
                "4. 语气友好简洁，用中文回答\n" +
                "5. 不知道的问题引导用户联系人工客服：400-xxx-xxxx";
    }

    /**
     * 带商品上下文的System Prompt
     */
    public static String buildProductSystemPrompt(String productContext) {
        String base = buildBaseSystemPrompt();
        if (productContext == null || productContext.isEmpty()) {
            return base;
        }
        return base + "\n\n【当前商品相关信息】\n" + productContext +
                "\n请基于以上信息回答用户关于该商品的问题。";
    }
}