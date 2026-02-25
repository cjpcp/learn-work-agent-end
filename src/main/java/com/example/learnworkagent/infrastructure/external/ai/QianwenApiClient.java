package com.example.learnworkagent.infrastructure.external.ai;

import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * 千问大模型API客户端
 */
@Slf4j
@Component
public class QianwenApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.qianwen.base-url:https://dashscope.aliyuncs.com}")
    private String baseUrl;

    @Value("${ai.qianwen.api-url:/compatible-mode/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.qianwen.api-key}")
    private String apiKey;

    @Value("${ai.qianwen.timeout}")
    private int timeout;

    @Value("${ai.qianwen.max-retries}")
    private int maxRetries;

    public QianwenApiClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
//                .baseUrl(baseUrl)
                .baseUrl("https://dashscope.aliyuncs.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * 调用千问API进行文本生成（非流式）
     */
    public Mono<String> generateText(String prompt) {

        //检测APIKey是否为空
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("千问API Key未配置");
            return Mono.error(new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI服务未配置"));
        }

        //封装用户参数
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        //封装是否流式输出等参数
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("enable_search", false);
        parameters.put("tool_choice", "none");
        parameters.put("incremental_output", true);

        //封装总的请求参数
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "qwen-plus");
        requestBody.put("messages", List.of(message));
        requestBody.put("stream", false);
        requestBody.put("parameters", parameters);

        //发送请求并返回返回值
        return webClient.post()
//                .uri(apiUrl)
                .uri("/compatible-mode/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof WebClientException)
                        .doBeforeRetry(retrySignal -> log.warn("重试AI API调用，第{}次", retrySignal.totalRetries() + 1)))
                .map(response -> {
                    log.info("千问API响应: {}", response);
                    if (response != null && response.containsKey("choices")) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            Map<String, Object> choice = choices.get(0);
                            if (choice.containsKey("message")) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> messageObj = (Map<String, Object>) choice.get("message");
                                if (messageObj != null && messageObj.containsKey("content")) {
                                    return (String) messageObj.get("content");
                                }
                            }
                        }
                    }
                    throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI服务响应格式错误");
                })
                .doOnError(error -> log.error("调用千问API失败", error))
                .onErrorResume(error -> {
                    if (error instanceof BusinessException) {
                        return Mono.error(error);
                    }
                    return Mono.error(new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI服务调用失败: " + error.getMessage()));
                });
    }

    /**
     * 调用千问API进行文本生成（流式）
     */
    public Flux<String> generateTextStream(String prompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("千问API Key未配置");
            return Flux.error(new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI服务未配置"));
        }

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "qwen-plus");
        requestBody.put("messages", List.of(message));
        requestBody.put("stream", true);

        return webClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMillis(timeout))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof WebClientException)
                        .doBeforeRetry(retrySignal -> log.warn("重试AI API调用，第{}次", retrySignal.totalRetries() + 1)))
                .flatMap(this::parseStreamChunk)
                .filter(content -> !content.isEmpty())
                .doOnError(error -> log.error("调用千问API失败", error))
                .onErrorResume(error -> {
                    if (error instanceof BusinessException) {
                        return Flux.error(error);
                    }
                    return Flux.error(new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI服务调用失败: " + error.getMessage()));
                });
    }

    /**
     * 解析流式响应块
     */
    private Flux<String> parseStreamChunk(String chunk) {
        try {
            String[] lines = chunk.split("\n");
            List<String> contents = new ArrayList<>();

            for (String line : lines) {
                log.info(line);
                line = line.trim();
                try {
                    Map<String, Object> response = objectMapper.readValue(line, Map.class);
                    if (response.containsKey("choices")) {
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            Map<String, Object> choice = choices.get(0);
                            if (choice.containsKey("delta")) {
                                Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                                if (delta != null && delta.containsKey("content")) {
                                    String content = (String) delta.get("content");
                                    if (content != null && !content.isEmpty()) {
                                        contents.add(content);
                                        log.info("解析到内容块: {}", content);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析流式响应失败: {}", line, e);
                }
            }

            if (!contents.isEmpty()) {
                return Flux.fromIterable(contents);
            }
            return Flux.empty();
        } catch (Exception e) {
            log.error("处理流式响应失败", e);
            return Flux.error(new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI服务调用失败: " + e.getMessage()));
        }
    }
}
