package com.example.learnworkagent.infrastructure.external.dify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


/**
 * Dify文件上传响应DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DifyFileUploadResponse {

    private String id;

    private String name;

    private Long size;

    private String extension;

    @JsonProperty("mime_type")
    private String mimeType;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("preview_url")
    private String previewUrl;

    @JsonProperty("source_url")
    private String sourceUrl;

    @JsonProperty("original_url")
    private String originalUrl;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("conversation_id")
    private String conversationId;

    @JsonProperty("file_key")
    private String fileKey;
}
