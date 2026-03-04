package com.example.learnworkagent.infrastructure.external.dify;

import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Dify文件上传服务
 */
@Slf4j
@Service
public class DifyFileUploadService {

    private final WebClient webClient;

    @Value("${dify.base-url:http://localhost}")
    private String baseUrl;

    @Value("${dify.api-key:app-IsYVRbyczVf9XOp4SXRkwm9m}")
    private String apiKey;

    @Value("${dify.timeout:30000}")
    private int timeout;

    @Value("${dify.max-retries:3}")
    private int maxRetries;

    public DifyFileUploadService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .build();
    }

    /**
     * 上传单个文件到Dify
     *
     * @param file 要上传的文件
     * @param user 用户标识
     * @return 上传响应
     */
    public Mono<DifyFileUploadResponse> uploadFile(MultipartFile file, String user) {
        if (file == null || file.isEmpty()) {
            return Mono.error(new BusinessException(ResultCode.PARAM_ERROR, "上传文件不能为空"));
        }

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Dify API Key未配置");
            return Mono.error(new BusinessException(ResultCode.SYSTEM_ERROR, "Dify服务未配置"));
        }

        try {
            byte[] fileBytes = file.getBytes();
            String filename = file.getOriginalFilename();
            String contentType = file.getContentType();
            
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            
            DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(fileBytes);
            org.springframework.core.io.Resource resource = DataBufferUtils.join(Mono.just(dataBuffer))
                    .map(buffer -> {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        DataBufferUtils.release(buffer);
                        return new org.springframework.core.io.ByteArrayResource(bytes) {
                            @Override
                            public String getFilename() {
                                return filename;
                            }
                        };
                    }).block();

            bodyBuilder.part("file", resource)
                    .filename(filename)
                    .contentType(MediaType.parseMediaType(contentType));
            
            bodyBuilder.part("user", user);

            return webClient.post()
                    .uri(baseUrl + "/v1/files/upload")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(DifyFileUploadResponse.class)
                    .timeout(Duration.ofMillis(timeout))
                    .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof WebClientException)
                            .doBeforeRetry(retrySignal -> log.warn("重试Dify文件上传，第{}次", retrySignal.totalRetries() + 1)))
                    .doOnSuccess(response -> log.info("Dify文件上传成功，文件ID: {}, 文件名: {}", response.getId(), response.getName()))
                    .doOnError(error -> log.error("Dify文件上传失败: {}", error.getMessage(), error))
                    .onErrorResume(error -> {
                        if (error instanceof BusinessException) {
                            return Mono.error(error);
                        }
                        return Mono.error(new BusinessException(ResultCode.SYSTEM_ERROR, "Dify文件上传失败: " + error.getMessage()));
                    });
        } catch (IOException e) {
            log.error("读取文件失败: {}", e.getMessage(), e);
            return Mono.error(new BusinessException(ResultCode.SYSTEM_ERROR, "读取文件失败: " + e.getMessage()));
        }
    }

    /**
     * 上传单个文件到Dify（使用默认用户标识）
     *
     * @param file 要上传的文件
     * @return 上传响应
     */
    public Mono<DifyFileUploadResponse> uploadFile(MultipartFile file) {
        return uploadFile(file, "default-user");
    }

    /**
     * 根据文件路径上传文件到Dify
     *
     * @param filePath 本地文件路径
     * @param user 用户标识
     * @return 上传响应
     */
    public Mono<DifyFileUploadResponse> uploadFileByPath(String filePath, String user) {
        if (filePath == null || filePath.isEmpty()) {
            return Mono.error(new BusinessException(ResultCode.PARAM_ERROR, "文件路径不能为空"));
        }

        java.io.File file = new java.io.File(filePath);
        if (!file.exists() || !file.isFile()) {
            return Mono.error(new BusinessException(ResultCode.PARAM_ERROR, "文件不存在: " + filePath));
        }

        String filename = file.getName();
        String contentType = getContentType(filename);
        long fileSize = file.length();

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(file);
        
        bodyBuilder.part("file", resource)
                .filename(filename)
                .contentType(MediaType.parseMediaType(contentType));
        
        bodyBuilder.part("user", user);

        return webClient.post()
                .uri(baseUrl + "/v1/files/upload")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(DifyFileUploadResponse.class)
                .timeout(Duration.ofMillis(timeout))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof WebClientException)
                        .doBeforeRetry(retrySignal -> log.warn("重试Dify文件上传，第{}次", retrySignal.totalRetries() + 1)))
                .doOnSuccess(response -> log.info("Dify文件上传成功，文件ID: {}, 文件名: {}", response.getId(), response.getName()))
                .doOnError(error -> log.error("Dify文件上传失败: {}", error.getMessage(), error))
                .onErrorResume(error -> {
                    if (error instanceof BusinessException) {
                        return Mono.error(error);
                    }
                    return Mono.error(new BusinessException(ResultCode.SYSTEM_ERROR, "Dify文件上传失败: " + error.getMessage()));
                });
    }

    /**
     * 根据文件路径上传文件到Dify（使用默认用户标识）
     *
     * @param filePath 本地文件路径
     * @return 上传响应
     */
    public Mono<DifyFileUploadResponse> uploadFileByPath(String filePath) {
        return uploadFileByPath(filePath, "default-user");
    }

    /**
     * 根据文件名获取ContentType
     */
    private String getContentType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt" -> "text/plain";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "zip" -> "application/zip";
            case "mp3" -> "audio/mpeg";
            case "mp4" -> "video/mp4";
            case "wav" -> "audio/wav";
            default -> "application/octet-stream";
        };
    }
}
