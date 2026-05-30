package com.seckill.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * AI聊天请求DTO
 */
@Data
public class ChatRequestDTO {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息长度不能超过2000字")
    private String message;

    /**
     * 当前浏览的商品ID，用于RAG检索商品上下文
     */
    private Long productId;

    /**
     * 未登录用户的会话ID，后端优先使用userId
     */
    private String sessionId;
}