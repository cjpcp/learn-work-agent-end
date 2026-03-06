package com.example.learnworkagent.infrastructure.external.dify;

import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dify聊天服务
 */
@Slf4j
@Service
public class DifyChatService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${dify.chat.base-url:http://localhost}")
    private String baseUrl;

    @Value("${dify.chat.api-key:app-WVk7LOKCEjRHeoaiPf536xKh}")
    private String chatApiKey;

    public DifyChatService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .build();
    }

    /**
     * 调用Dify聊天API进行智能咨询（流式响应）
     *
     * @param query          用户问题
     * @param fileUrls       文件URL列表（可选）
     * @param conversationId 对话ID（可选）
     * @param user           用户标识
     * @return 流式响应
     */
    public Flux<String> chatStream(String query, List<String> fileUrls, String conversationId, String user) {
        if (query == null || query.isEmpty()) {
            return Flux.error(new BusinessException(ResultCode.PARAM_ERROR, "问题内容不能为空"));
        }

        if (chatApiKey == null || chatApiKey.isEmpty()) {
            log.warn("Dify Chat API Key未配置");
            return Flux.error(new BusinessException(ResultCode.SYSTEM_ERROR, "Dify服务未配置"));
        }

        Map<String, Object> requestBody = buildChatRequestBody(query, fileUrls, conversationId, user);

        log.info("调用Dify聊天API，query: {}, user: {}, 文件数: {}", query, user, 
                fileUrls != null ? fileUrls.size() : 0);
        log.debug("请求体: {}", requestBody);

        return webClient.post()
                .uri(baseUrl + "/v1/chat-messages")
                .header("Authorization", "Bearer " + chatApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .map(chunk -> {
                    try {
                        // 处理SSE格式数据，保留内容中的换行符
                        String jsonStr = chunk;
                        if (chunk.startsWith("data:")) {
                            // 只移除"data:"前缀，不trim以保留内容中的换行和空格
                            jsonStr = chunk.substring(5);
                        }
                        
                        // 跳过[DONE]标记
                        if ("[DONE]".equals(jsonStr)) {
                            return "";
                        }
                        
                        JsonNode jsonNode = objectMapper.readTree(jsonStr);
                        
                        // Dify流式响应中，delta.text包含增量内容
                        JsonNode eventNode = jsonNode.get("event");
                        if (eventNode != null && "message".equals(eventNode.asText())) {
                            JsonNode answerNode = jsonNode.get("answer");
                            if (answerNode != null && answerNode.isTextual()) {
                                String answer = answerNode.asText();
                                // 只返回非空的新增内容
                                if (!answer.isEmpty()) {
                                    return answer;
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("解析Dify返回数据失败，chunk: {}", chunk, e);
                    }
                    return "";
                })
                .filter(chunk -> !chunk.isEmpty())
                .distinct() // 去重，防止重复内容
                .doOnNext(chunk -> log.debug("接收到流式数据chunk: {}", chunk))
                .doOnComplete(() -> log.info("Dify聊天API调用完成，user: {}", user))
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException wcre) {
                        log.error("调用Dify聊天API失败 - 状态码: {}, 响应体: {}",
                                wcre.getStatusCode(), wcre.getResponseBodyAsString());
                    } else {
                        log.error("调用Dify聊天API失败", error);
                    }
                })
                .onErrorResume(error -> {
                    if (error instanceof BusinessException) {
                        return Flux.error(error);
                    }
                    if (error instanceof WebClientResponseException wcre) {
                        String errorMsg = "Dify聊天API调用失败 - 状态码: " + wcre.getStatusCode() +
                                ", 响应: " + wcre.getResponseBodyAsString();
                        return Flux.error(new BusinessException(ResultCode.SYSTEM_ERROR, errorMsg));
                    }
                    return Flux.error(new BusinessException(ResultCode.SYSTEM_ERROR, "Dify聊天API调用失败: " + error.getMessage()));
                });
    }

    /**
     * 构建聊天请求体
     */
    private Map<String, Object> buildChatRequestBody(String query, List<String> fileUrls, String conversationId, String user) {
        Map<String, Object> requestBody = new HashMap<>();

        // 构建inputs字段（必需）
        List<Map<String, Object>> inputs = new ArrayList<>();
        if (fileUrls != null && !fileUrls.isEmpty()) {
            for (String url : fileUrls) {
                Map<String, Object> input = new HashMap<>();
                input.put("transfer_method", "remote_url");
                input.put("url", url);
                input.put("type", determineFileType(url));
                inputs.add(input);
            }
        }

        Map<String, Object> inputsWrapper = new HashMap<>();
        inputsWrapper.put("input", inputs);
        requestBody.put("inputs", inputsWrapper);

        requestBody.put("query", query);
        requestBody.put("response_mode", "streaming");
        requestBody.put("conversation_id", conversationId != null ? conversationId : "");
        requestBody.put("user", user);

        return requestBody;
    }

    /**
     * 根据URL判断文件类型
     *
     * @param url 文件URL
     * @return 文件类型（image、document或audio）
     */
    private String determineFileType(String url) {
        if (url == null || url.isEmpty()) {
            return "document";
        }

        String lowerUrl = url.toLowerCase();
        String[] imageExtensions = {"jpg", "jpeg", "png", "gif", "webp", "bmp", "tiff", "svg"};
        String[] audioExtensions = {"mp3", "wav", "ogg", "m4a", "flac", "aac", "wma"};

        for (String ext : imageExtensions) {
            if (lowerUrl.endsWith("." + ext)) {
                return "image";
            }
        }

        for (String ext : audioExtensions) {
            if (lowerUrl.endsWith("." + ext)) {
                return "audio";
            }
        }

        return "document";
    }

    /**
     * 调用Dify聊天API进行智能咨询（流式响应，使用默认用户标识）
     *
     * @param query          用户问题
     * @param fileUrls       文件URL列表（可选）
     * @param conversationId 对话ID（可选）
     * @return 流式响应
     */
    public Flux<String> chatStream(String query, List<String> fileUrls, String conversationId) {
        return chatStream(query, fileUrls, conversationId, "default-user");
    }

    /**
     * 从文件输入列表中提取URL列表
     *
     * @param fileInputs 文件输入列表
     * @return URL列表
     */
    public List<String> extractFileUrls(List<com.example.learnworkagent.domain.consultation.dto.DifyConsultationRequest.FileInput> fileInputs) {
        if (fileInputs == null || fileInputs.isEmpty()) {
            return null;
        }
        return fileInputs.stream()
                .map(com.example.learnworkagent.domain.consultation.dto.DifyConsultationRequest.FileInput::getUrl)
                .toList();
    }
}
