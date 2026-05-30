package com.seckill.dto.ai;

import lombok.Data;

/**
 * AI聊天响应DTO
 * 当前Controller仍返回Result<String>，此DTO预留用于后续扩展（如流式输出、带元信息的返回）
 */
@Data
public class ChatResponseDTO {

    /**
     * AI回复内容
     */
    private String content;

    /**
     * 是否命中缓存
     */
    private Boolean cached = false;

    /**
     * 关联的商品ID
     */
    private Long productId;

    /**
     * 用户标识
     */
    private String identifier;

    /**
     * 当前对话轮数
     */
    private Integer historyRounds;
}