package com.example.learnworkagent.infrastructure.external.dify;

import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

/**
 * Dify语音转文字服务
 * 调用 Dify 工作流接口（POST /v1/workflows/run，blocking模式），
 * 将语音文件URL传入工作流，同步获取转写后的文字结果。
 * 响应路径：data.outputs.result（JSON字符串）-> text字段
 */
@Slf4j
@Service
public class SpeechToTextService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 语音转文字工作流的基础URL */
    @Value("${dify.speech-to-text.base-url:http://159.75.50.89}")
    private String baseUrl;

    /** 语音转文字工作流的API Key */
    @Value("${dify.speech-to-text.api-key:app-gZzb8H3DvK2D3Er4HLp7QZqZ}")
    private String apiKey;

    /**
     * 调用Dify工作流将语音URL同步转换为文字。
     * 工作流接口：POST /v1/workflows/run，blocking模式。
     * 请求体：{ "inputs": { "file_url": "<voiceUrl>" }, "response_mode": "blocking", "user": "<user>" }
     * 响应解析路径：data.outputs.result（JSON字符串）-> text
     *
     * @param voiceUrl 语音文件的公网访问URL（OSS地址）
     * @param user     用户标识
     * @return 转写后的文字内容；若转写失败则抛出 BusinessException
     */
    public String convertVoiceUrlToText(String voiceUrl, String user) {
        if (voiceUrl == null || voiceUrl.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "语音URL不能为空");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "Dify语音转文字服务未配置");
        }

        // 构建工作流请求体，与curl示例保持一致
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("file_url", voiceUrl);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", inputs);
        requestBody.put("response_mode", "blocking");
        requestBody.put("user", user);

        log.info("调用语音转文字工作流，voiceUrl: {}, user: {}", voiceUrl, user);

        try {
            // 使用RestClient发起同步HTTP请求
            RestClient restClient = RestClient.builder()
                    .baseUrl(baseUrl)
                    .build();

            String responseBody = restClient.post()
                    .uri("/v1/workflows/run")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                log.warn("语音转文字工作流返回空响应，voiceUrl: {}", voiceUrl);
                return "";
            }

            return parseTextFromResponse(responseBody, voiceUrl);

        } catch (RestClientException e) {
            log.error("调用语音转文字工作流HTTP请求失败，voiceUrl: {}", voiceUrl, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR,
                    "语音转文字工作流调用失败: " + e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("语音转文字处理异常，voiceUrl: {}", voiceUrl, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR,
                    "语音转文字处理异常: " + e.getMessage());
        }
    }

    /**
     * 解析工作流响应，提取文字结果。
     * 响应路径：data.outputs.result（JSON字符串）-> text
     *
     * @param responseBody 工作流HTTP响应体（JSON字符串）
     * @param voiceUrl     语音URL（仅用于日志）
     * @return 转写的文字内容
     */
    private String parseTextFromResponse(String responseBody, String voiceUrl) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // 路径：data.outputs.result
            JsonNode resultNode = root.path("data").path("outputs").path("result");
            if (resultNode.isMissingNode() || !resultNode.isTextual()) {
                log.warn("语音转文字响应中未找到 data.outputs.result 字段，voiceUrl: {}, 响应: {}",
                        voiceUrl, responseBody);
                return "";
            }

            // result 是一个 JSON 字符串，需要再次解析，提取其中的 text 字段
            String resultJson = resultNode.asText();
            JsonNode resultObj = objectMapper.readTree(resultJson);
            JsonNode textNode = resultObj.path("text");

            if (textNode.isMissingNode() || !textNode.isTextual()) {
                log.warn("语音转文字 result 中未找到 text 字段，voiceUrl: {}, result: {}",
                        voiceUrl, resultJson);
                return "";
            }

            String text = textNode.asText().strip();
            log.info("语音转文字成功，voiceUrl: {}, 文字长度: {}", voiceUrl, text.length());
            return text;

        } catch (Exception e) {
            log.error("解析语音转文字工作流响应失败，voiceUrl: {}, 响应: {}", voiceUrl, responseBody, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR,
                    "解析语音转文字响应失败: " + e.getMessage());
        }
    }
}