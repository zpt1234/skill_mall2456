package com.seckill.controller;

import com.seckill.dto.ChatRequestDTO;
import com.seckill.result.Result;
import com.seckill.service.AiAgentService;
import com.seckill.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@Validated
public class AiAgentController {

    @Autowired
    private AiAgentService aiAgentService;

    /**
     * 普通聊天（非流式）
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody @Validated ChatRequestDTO requestDTO, HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String response = aiAgentService.chat(userId, requestDTO.getSessionId(), requestDTO);
        return Result.success(response);
    }

    /**
     * 流式聊天（SSE）
     * 前端通过 EventSource 或 fetch 连接
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam @NotBlank String message,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String token,  // ← 新增：从URL参数取token
            HttpServletRequest request) {

        // 如果header里没有token，尝试从参数取（兼容EventSource）
        if (token != null && !token.isEmpty()) {
            request.setAttribute("token", token);
        }

        Long userId = UserContext.getUserId(); // 你的UserContext需要支持从attribute取token
        ChatRequestDTO dto = new ChatRequestDTO();
        dto.setMessage(message);
        dto.setProductId(productId);
        dto.setSessionId(sessionId);

        return aiAgentService.streamChat(userId, sessionId, dto);
    }
}