package com.example.learnworkagent.infrastructure.external.dify;

import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.function.Consumer;

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
     * 调用Dify聊天API进行智能咨询（流式响应），并捕获conversation_id
     *
     * @param query          用户问题
     * @param fileUrls       文件URL列表（可选）
     * @param conversationId 对话ID（可选）
     * @param user           用户标识
     * @param conversationIdConsumer 回调，用于返回conversation_id（可选）
     * @return 流式响应
     */
    public Flux<String> chatStream(String query, List<String> fileUrls, String conversationId, String user,
                                   Consumer<String> conversationIdConsumer) {
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

        final boolean[] conversationIdCaptured = {false};

        return webClient.post()
                .uri(baseUrl + "/v1/chat-messages")
                .header("Authorization", "Bearer " + chatApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .map(chunk -> {
                    try {
                        String jsonStr = chunk;
                        if (chunk.startsWith("data:")) {
                            jsonStr = chunk.substring(5);
                        }

                        if ("[DONE]".equals(jsonStr)) {
                            return "";
                        }

                        JsonNode jsonNode = objectMapper.readTree(jsonStr);

                        if (!conversationIdCaptured[0]) {
                            JsonNode convIdNode = jsonNode.get("conversation_id");
                            if (convIdNode != null && !convIdNode.isNull()) {
                                String convId = convIdNode.asText();
                                if (convId != null && !convId.isEmpty()) {
                                    conversationIdCaptured[0] = true;
                                    if (conversationIdConsumer != null) {
                                        conversationIdConsumer.accept(convId);
                                    }
                                    log.info("捕获到Dify conversation_id: {}", convId);
                                }
                            }
                        }

                        JsonNode eventNode = jsonNode.get("event");
                        if (eventNode != null && "message".equals(eventNode.asText())) {
                            JsonNode answerNode = jsonNode.get("answer");
                            if (answerNode != null && answerNode.isTextual()) {
                                String answer = answerNode.asText();
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
     * 获取对话历史消息
     *
     * @param conversationId 对话ID
     * @param user           用户标识
     * @return 消息列表
     */
    public List<Map<String, Object>> getConversationMessages(String conversationId, String user) {
        if (conversationId == null || conversationId.isEmpty()) {
            return Collections.emptyList();
        }

        if (chatApiKey == null || chatApiKey.isEmpty()) {
            log.warn("Dify Chat API Key未配置");
            return Collections.emptyList();
        }

        try {
            String url = baseUrl + "/v1/messages?conversation_id=" + conversationId + "&user=" + user + "&limit=20";

            log.info("调用Dify获取对话历史，conversation_id: {}, user: {}", conversationId, user);

            String response = webClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + chatApiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("Dify对话历史响应: {}", response);

            List<Map<String, Object>> messages = new ArrayList<>();
            if (response != null && !response.isEmpty()) {
                JsonNode rootNode = objectMapper.readTree(response);
                JsonNode dataNode = rootNode.get("data");
                if (dataNode != null && dataNode.isArray()) {
                    for (JsonNode node : dataNode) {
                        Map<String, Object> msg = new HashMap<>();
                        msg.put("id", getTextValue(node, "id"));
                        msg.put("conversation_id", getTextValue(node, "conversation_id"));
                        msg.put("inputs", node.get("inputs"));
                        msg.put("query", getTextValue(node, "query"));
                        msg.put("answer", getTextValue(node, "answer"));
                        msg.put("message_type", getTextValue(node, "message_type"));
                        msg.put("created_at", getTextValue(node, "created_at"));
                        messages.add(msg);
                    }
                }
            }
            return messages;
        } catch (Exception e) {
            log.error("获取Dify对话历史失败，conversation_id: {}", conversationId, e);
            return Collections.emptyList();
        }
    }

    private String getTextValue(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
    }

    /**
     * 构建聊天请求体
     */
    private Map<String, Object> buildChatRequestBody(String query, List<String> fileUrls, String conversationId, String user) {
        Map<String, Object> requestBody = new HashMap<>();

        // inputs 固定为空对象（必填字段）
        requestBody.put("inputs", new HashMap<>());

        // 文件放在顶层 files 数组（Dify chat-messages API 规范）
        if (fileUrls != null && !fileUrls.isEmpty()) {
            List<Map<String, Object>> files = new ArrayList<>();
            for (String url : fileUrls) {
                Map<String, Object> fileObj = new HashMap<>();
                fileObj.put("transfer_method", "remote_url");
                fileObj.put("url", url);
                fileObj.put("type", determineFileType(url));
                files.add(fileObj);
            }
            requestBody.put("files", files);
        }

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

}
