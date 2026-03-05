package com.example.learnworkagent.domain.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Dify智能咨询请求DTO
 */
@Data
public class DifyConsultationRequest {

    @NotBlank(message = "问题内容不能为空")
    private String query;

    /**
     * 文件列表（可选）
     */
    private List<FileInput> files;

    /**
     * 对话ID（可选，用于多轮对话）
     */
    private String conversationId;

    /**
     * 文件输入信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInput {
        private String transferMethod;
        private String url;
        private String type;
    }
}
