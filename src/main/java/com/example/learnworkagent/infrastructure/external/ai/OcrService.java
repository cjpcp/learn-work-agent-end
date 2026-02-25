package com.example.learnworkagent.infrastructure.external.ai;

import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OCR 服务 - 使用千问 API 进行文档识别
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {

    @Value("${ai.qianwen.api-key}")
    private String apiKey;

    /**
     * 识别文档内容并判断文档类型
     *
     * @param fileUrl 文件 URL
     * @return 文档类型（成绩单、推荐信、家庭情况证明、收入证明等）
     */
    public Mono<String> identifyDocumentType(String fileUrl) {
        String prompt = """
                请分析这个文档的内容，判断它是什么类型的文档。
                可能的文档类型包括：
                1. 成绩单 - 包含课程名称、成绩、学分等信息
                2. 推荐信 - 包含推荐人对学生的评价和推荐意见
                3. 家庭情况证明 - 包含家庭成员、经济状况等信息
                4. 收入证明 - 包含收入金额、工作单位等信息
                5. 其他 - 不属于上述任何类型
                
                请只返回文档类型名称（如：成绩单、推荐信、家庭情况证明、收入证明、其他），不要返回其他内容。""";

        return analyzeDocument(fileUrl, prompt);
    }

    /**
     * 检查文档是否为指定类型
     *
     * @param fileUrl      文件 URL
     * @param expectedType 期望的文档类型
     * @return 是否为期望的文档类型
     */
    public Mono<Boolean> checkDocumentType(String fileUrl, String expectedType) {
        return identifyDocumentType(fileUrl)
                .map(documentType -> {
                    log.info("文档 {} 识别为: {}, 期望类型: {}", fileUrl, documentType, expectedType);
                    return documentType.contains(expectedType) || expectedType.contains(documentType);
                })
                .onErrorResume(error -> {
                    log.error("检查文档类型失败: {}", fileUrl, error);
                    return Mono.just(false);
                });
    }

    /**
     * 分析文档内容
     *
     * @param fileUrl 文件 URL
     * @param prompt  分析提示词
     * @return 分析结果
     */
    private Mono<String> analyzeDocument(String fileUrl, String prompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("千问 API Key 未配置");
            return Mono.error(new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI 服务未配置"));
        }

        log.info("开始分析文档，URL: {}", fileUrl);

        WebClient webClient = WebClient.builder()
                .baseUrl("https://dashscope.aliyuncs.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");

        Map<String, String> image = new HashMap<>();
        image.put("image", fileUrl);

        message.put("content", List.of(prompt, image));

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("enable_search", false);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "qwen-vl-plus");
        requestBody.put("messages", List.of(message));
        requestBody.put("stream", false);
        requestBody.put("parameters", parameters);

        log.debug("请求体: {}", requestBody);

        return webClient.post()
                .uri("/compatible-mode/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    log.info("千问 OCR API 响应: {}", response);
                    if (response != null && response.containsKey("choices")) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            Map<String, Object> choice = choices.get(0);
                            if (choice.containsKey("message")) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> messageObj = (Map<String, Object>) choice.get("message");
                                if (messageObj != null && messageObj.containsKey("content")) {
                                    String content = (String) messageObj.get("content");
                                    return content.trim();
                                }
                            }
                        }
                    }
                    throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI 服务响应格式错误");
                })
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException wcre = (WebClientResponseException) error;
                        log.error("调用千问 OCR API 失败 - 状态码: {}, 响应体: {}", 
                                wcre.getStatusCode(), wcre.getResponseBodyAsString());
                    } else {
                        log.error("调用千问 OCR API 失败", error);
                    }
                })
                .onErrorResume(error -> {
                    if (error instanceof BusinessException) {
                        return Mono.error(error);
                    }
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException wcre = (WebClientResponseException) error;
                        String errorMsg = "OCR 服务调用失败 - 状态码: " + wcre.getStatusCode() + 
                                ", 响应: " + wcre.getResponseBodyAsString();
                        return Mono.error(new BusinessException(ResultCode.AI_SERVICE_ERROR, errorMsg));
                    }
                    return Mono.error(new BusinessException(ResultCode.AI_SERVICE_ERROR, "OCR 服务调用失败: " + error.getMessage()));
                });
    }
}
