package com.seckill.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.dto.ChatRequestDTO;
import com.seckill.service.AiAgentService;
import com.seckill.service.RagService;
import com.seckill.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class AiAgentServiceImpl implements AiAgentService {

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.api.url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;

    @Value("${ai.model:deepseek-chat}")
    private String model;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    @Qualifier("aiRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("aiWebClient")
    private WebClient webClient;

    @Autowired
    private RagService ragService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_HISTORY_ROUNDS = 3;
    private static final long HISTORY_EXPIRE_HOURS = 24;
    private static final long QUERY_CACHE_MINUTES = 5;

    // ==================== 非流式聊天 ====================

    @Override
    public String chat(Long userId, String sessionId, ChatRequestDTO request) {
        String message = request.getMessage();
        Long productId = request.getProductId();
        String identifier = buildIdentifier(userId, sessionId);

        String cacheKey = RedisKeyUtil.AI_CHAT_CACHE + DigestUtils.md5Hex(message + ":" + productId);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("AI缓存命中|identifier={}", identifier);
            return cached;
        }

        try {
            List<Map<String, String>> messages = buildMessages(identifier, message, productId);
            String answer = callDeepSeek(messages);
            saveHistory(identifier, message, answer);
            stringRedisTemplate.opsForValue().set(cacheKey, answer, QUERY_CACHE_MINUTES, TimeUnit.MINUTES);
            return answer;

        } catch (ResourceAccessException e) {
            log.error("AI服务网络超时|identifier={}", identifier, e);
            return "当前咨询人数较多，请稍后重试";
        } catch (Exception e) {
            log.error("AI调用失败|identifier={}", identifier, e);
            return "服务暂时繁忙，请稍后重试";
        }
    }

    // ==================== 流式聊天（SSE） ====================

    @Override
    public SseEmitter streamChat(Long userId, String sessionId, ChatRequestDTO request) {
        String message = request.getMessage();
        Long productId = request.getProductId();
        String identifier = buildIdentifier(userId, sessionId);

        SseEmitter emitter = new SseEmitter(120_000L);
        StringBuilder fullContent = new StringBuilder();
        AtomicBoolean completed = new AtomicBoolean(false);

        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, String>> messages = buildMessages(identifier, message, productId);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", model);
                requestBody.put("messages", messages);
                requestBody.put("temperature", 0.7);
                requestBody.put("max_tokens", 1000);
                requestBody.put("stream", true);

                webClient.post()
                        .uri(apiUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .onStatus(HttpStatus::isError, clientResponse -> {
                            log.error("AI流式接口返回错误状态码: {}", clientResponse.statusCode());
                            return Mono.error(new RuntimeException("AI服务异常"));
                        })
                        .bodyToFlux(String.class)
                        .subscribe(
                                line -> {
                                    if (completed.get()) return;
                                    try {
                                        if (line == null || line.isEmpty()) return;

                                        String data = line;
                                        if (line.startsWith("data: ")) {
                                            data = line.substring(6);
                                        }

                                        if ("[DONE]".equals(data)) {
                                            finishStream(emitter, completed, identifier, message, fullContent.toString());
                                            return;
                                        }

                                        Map<String, Object> map = objectMapper.readValue(data, Map.class);
                                        List<Map<String, Object>> choices = (List<Map<String, Object>>) map.get("choices");

                                        if (choices != null && !choices.isEmpty()) {
                                            Map<String, Object> choice = choices.get(0);

                                            String content = null;
                                            Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                                            if (delta != null) {
                                                // 【修正】只取 content，不取 reasoning_content
                                                content = (String) delta.get("content");
                                            }

                                            // 兜底：非流式格式
                                            if (content == null) {
                                                Map<String, Object> msg = (Map<String, Object>) choice.get("message");
                                                if (msg != null) {
                                                    content = (String) msg.get("content");
                                                }
                                            }

                                            // 只推送正式回复内容
                                            if (content != null && !content.isEmpty()) {
                                                fullContent.append(content);
                                                emitter.send(SseEmitter.event()
                                                        .name("message")
                                                        .data(content));
                                            }

                                            String finishReason = (String) choice.get("finish_reason");
                                            if ("stop".equals(finishReason) || "length".equals(finishReason)) {
                                                finishStream(emitter, completed, identifier, message, fullContent.toString());
                                            }
                                        }

                                    } catch (Exception e) {
                                        log.warn("SSE行解析失败|line={}", line, e);
                                    }
                                },
                                // ... error 和 complete 回调不变
                                error -> {
                                    log.error("SSE流式调用失败|identifier={}", identifier, error);
                                    if (!completed.get()) {
                                        completed.set(true);
                                        try {
                                            emitter.send(SseEmitter.event()
                                                    .name("error")
                                                    .data("服务暂时繁忙，请稍后重试"));
                                        } catch (IOException ex) {
                                            log.error("发送错误事件失败", ex);
                                        }
                                        emitter.completeWithError(error);
                                    }
                                },
                                () -> {
                                    log.info("SSE流正常完成|identifier={}", identifier);
                                    if (!completed.get()) {
                                        finishStream(emitter, completed, identifier, message, fullContent.toString());
                                    }
                                }
                        );

            } catch (Exception e) {
                log.error("SSE初始化失败|identifier={}", identifier, e);
                if (!completed.get()) {
                    completed.set(true);
                    emitter.completeWithError(e);
                }
            }
        });

        emitter.onCompletion(() -> log.info("SSE连接关闭|identifier={}", identifier));
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时|identifier={}", identifier);
            if (!completed.get()) {
                completed.set(true);
                emitter.complete();
            }
        });
        emitter.onError(e -> log.error("SSE连接异常|identifier={}", identifier, e));

        return emitter;
    }

    /**
     * 结束流式输出并保存历史
     */
    private void finishStream(SseEmitter emitter, AtomicBoolean completed, String identifier,
                              String userMessage, String fullAnswer) {
        if (completed.get()) {
            log.warn("重复调用finishStream|identifier={}", identifier);
            return;
        }
        completed.set(true);

        log.info("完成AI流式响应|identifier={}|answerLength={}|answer={}", identifier, fullAnswer.length(),
                fullAnswer.length() > 100 ? fullAnswer.substring(0, 100) + "..." : fullAnswer);

        try {
            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
            log.info("发送done事件成功|identifier={}", identifier);
        } catch (IOException e) {
            log.error("发送done事件失败|identifier={}", identifier, e);
        }

        saveHistory(identifier, userMessage, fullAnswer);
        emitter.complete();
        log.info("SSE流完成|identifier={}", identifier);
    }

    // ==================== 公共方法 ====================

    private String buildIdentifier(Long userId, String sessionId) {
        if (userId != null) {
            return "user:" + userId;
        }
        if (sessionId != null && !sessionId.isEmpty()) {
            return "session:" + sessionId;
        }
        return "anonymous:" + System.currentTimeMillis();
    }

    private List<Map<String, String>> buildMessages(String identifier, String currentMessage, Long productId) {
        List<Map<String, String>> messages = new ArrayList<>();

        String systemContent = buildSystemPrompt(productId);
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemContent);
        messages.add(systemMsg);

        // 只有有有效标识符才加载历史
        if (identifier != null && !identifier.startsWith("anonymous:")) {
            List<String> history = getHistory(identifier);
            for (String historyJson : history) {
                try {
                    Map<String, String> historyMsg = objectMapper.readValue(historyJson, Map.class);
                    messages.add(historyMsg);
                } catch (JsonProcessingException e) {
                    log.warn("历史消息解析失败，跳过");
                }
            }
        }

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", currentMessage);
        messages.add(userMsg);

        return messages;
    }

    private String buildSystemPrompt(Long productId) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是秒杀商城的智能客服助手。你的职责：\n");
        sb.append("1. 帮助用户解答订单相关问题（查询、支付、取消、退款）\n");
        sb.append("2. 解答秒杀活动规则（时间、库存、限购）\n");
        sb.append("3. 处理常见技术问题（登录、页面加载）\n");
        sb.append("4. 语气友好简洁，用中文回答\n");
        sb.append("5. 不知道的问题引导用户联系人工客服：400-xxx-xxxx\n");

        if (productId != null) {
            String productContext = ragService.retrieve(productId, null);
            if (productContext != null && !productContext.isEmpty()) {
                sb.append("\n【当前商品相关信息】\n");
                sb.append(productContext);
                sb.append("\n请基于以上信息回答用户关于该商品的问题。");
            }
        }

        return sb.toString();
    }

    private String callDeepSeek(List<Map<String, String>> messages) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1000);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        org.springframework.http.HttpEntity<Map<String, Object>> entity =
                new org.springframework.http.HttpEntity<>(requestBody, headers);
        Map<String, Object> response = restTemplate.postForObject(apiUrl, entity, Map.class);

        if (response == null) {
            throw new RuntimeException("AI服务返回空响应");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("AI服务返回空choices");
        }

        Map<String, Object> messageMap = (Map<String, Object>) choices.get(0).get("message");
        if (messageMap == null) {
            throw new RuntimeException("AI服务返回空message");
        }

        String content = (String) messageMap.get("content");
        if (content == null) {
            throw new RuntimeException("AI服务返回空content");
        }

        return content;
    }

    private List<String> getHistory(String identifier) {
        if (identifier == null) {
            return new ArrayList<>();
        }
        String key = RedisKeyUtil.AI_CHAT_HISTORY + identifier;
        List<String> history = stringRedisTemplate.opsForList().range(key, 0, -1);
        return history != null ? history : new ArrayList<>();
    }

    private void saveHistory(String identifier, String userMessage, String aiResponse) {
        if (identifier == null || identifier.startsWith("anonymous:")) {
            return;
        }

        try {
            String key = RedisKeyUtil.AI_CHAT_HISTORY + identifier;

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            Map<String, String> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", aiResponse);

            String userMsgJson = objectMapper.writeValueAsString(userMsg);
            String assistantMsgJson = objectMapper.writeValueAsString(assistantMsg);

            stringRedisTemplate.opsForList().rightPushAll(key, userMsgJson, assistantMsgJson);

            Long currentSize = stringRedisTemplate.opsForList().size(key);
            if (currentSize != null && currentSize > MAX_HISTORY_ROUNDS * 2) {
                stringRedisTemplate.opsForList().trim(key, -MAX_HISTORY_ROUNDS * 2, -1);
            }

            stringRedisTemplate.expire(key, HISTORY_EXPIRE_HOURS, TimeUnit.HOURS);

        } catch (JsonProcessingException e) {
            log.error("保存对话历史失败|identifier={}", identifier, e);
        }
    }
}