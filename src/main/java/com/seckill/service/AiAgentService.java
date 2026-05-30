package com.seckill.service;

import com.seckill.dto.ChatRequestDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiAgentService {

    /**
     * 普通聊天（非流式）
     */
    String chat(Long userId, String sessionId, ChatRequestDTO request);

    /**
     * 流式聊天（SSE）
     */
    SseEmitter streamChat(Long userId, String sessionId, ChatRequestDTO request);
}