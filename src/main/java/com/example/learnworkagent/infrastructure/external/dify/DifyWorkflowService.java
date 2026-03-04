package com.example.learnworkagent.infrastructure.external.dify;

import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import org.springframework.core.ParameterizedTypeReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dify工作流服务
 */
@Slf4j
@Service
public class DifyWorkflowService {

    private final WebClient webClient;

    @Value("${dify.base-url:http://localhost}")
    private String baseUrl;

    @Value("${dify.api-key:app-n6sVBytOtdXBvScrUbBXjq1B}")
    private String apiKey;

    @Value("${dify.timeout:30000}")
    private int timeout;

    public DifyWorkflowService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .build();
    }

    /**
     * 调用Dify工作流识别文档
     *
     * @param fileUrls 文件URL列表
     * @param user 用户标识
     * @return 识别结果
     */
    public Mono<Map<String, Object>> identifyDocuments(List<String> fileUrls, String user) {
        if (fileUrls == null || fileUrls.isEmpty()) {
            return Mono.error(new BusinessException(ResultCode.PARAM_ERROR, "文件列表不能为空"));
        }

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Dify API Key未配置");
            return Mono.error(new BusinessException(ResultCode.SYSTEM_ERROR, "Dify服务未配置"));
        }

        Map<String, Object> requestBody = getStringObjectMap(fileUrls, user);

        log.info("调用Dify工作流识别文档，文件数: {}", fileUrls.size());
        log.debug("请求体: {}", requestBody);

        return webClient.post()
                .uri(baseUrl + "/v1/workflows/run")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .doOnSuccess(response -> log.info("Dify工作流调用成功: {}", response))
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException wcre) {
                        log.error("调用Dify工作流失败 - 状态码: {}, 响应体: {}",
                                wcre.getStatusCode(), wcre.getResponseBodyAsString());
                    } else {
                        log.error("调用Dify工作流失败", error);
                    }
                })
                .onErrorResume(error -> {
                    if (error instanceof BusinessException) {
                        return Mono.error(error);
                    }
                    if (error instanceof WebClientResponseException wcre) {
                        String errorMsg = "Dify工作流调用失败 - 状态码: " + wcre.getStatusCode() +
                                ", 响应: " + wcre.getResponseBodyAsString();
                        return Mono.error(new BusinessException(ResultCode.SYSTEM_ERROR, errorMsg));
                    }
                    return Mono.error(new BusinessException(ResultCode.SYSTEM_ERROR, "Dify工作流调用失败: " + error.getMessage()));
                });
    }

    private static Map<String, Object> getStringObjectMap(List<String> fileUrls, String user) {
        List<Map<String, Object>> files = new ArrayList<>();
        for (String url : fileUrls) {
            Map<String, Object> file = new HashMap<>();
            file.put("transfer_method", "remote_url");
            file.put("url", url);
            file.put("type", determineFileType(url));
            files.add(file);
        }

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("file", files);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", inputs);
        requestBody.put("response_mode", "blocking");
        requestBody.put("user", user);
        return requestBody;
    }

    /**
     * 根据URL判断文件类型
     *
     * @param url 文件URL
     * @return 文件类型（image或document）
     */
    private static String determineFileType(String url) {
        if (url == null || url.isEmpty()) {
            return "document";
        }

        // 提取URL中的文件扩展名
        String lowerUrl = url.toLowerCase();
        
        // 图片文件扩展名
        String[] imageExtensions = {"jpg", "jpeg", "png", "gif", "webp", "bmp", "tiff", "svg"};
        
        for (String ext : imageExtensions) {
            if (lowerUrl.endsWith("." + ext)) {
                return "image";
            }
        }
        
        // 其他文件默认为document
        return "document";
    }

    /**
     * 调用Dify工作流识别文档（使用默认用户标识）
     *
     * @param fileUrls 文件URL列表
     * @return 识别结果
     */
    public Mono<Map<String, Object>> identifyDocuments(List<String> fileUrls) {
        return identifyDocuments(fileUrls, "default-user");
    }
}