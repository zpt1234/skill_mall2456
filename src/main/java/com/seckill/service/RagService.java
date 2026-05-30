package com.seckill.service;

/**
 * RAG检索服务
 */
public interface RagService {

    /**
     * 根据商品ID和用户问题检索相关知识
     *
     * @param productId 商品ID
     * @param query     用户查询
     * @return 检索到的上下文文本，为空表示无相关知识
     */
    String retrieve(Long productId, String query);
}