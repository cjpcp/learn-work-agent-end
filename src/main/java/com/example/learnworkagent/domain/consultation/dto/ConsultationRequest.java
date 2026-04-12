package com.example.learnworkagent.domain.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 咨询请求DTO.
 * <p>封装用户提交咨询问题的请求参数.</p>
 *
 * @author system
 */
@Data
public class ConsultationRequest {

    /**
     * 问题文本内容（纯语音提问时可为空）
     */
    private String questionText;

    /**
     * 问题类型（TEXT-文本, VOICE-语音, IMAGE-图片）
     */
    @NotBlank(message = "问题类型不能为空")
    private String questionType;

    /**
     * 问题分类（AWARD-奖助勤贷, DORM-宿舍管理, DISCIPLINE-违纪申诉, MENTAL-心理健康, EMPLOYMENT-就业指导）
     */
    private String category;

    /**
     * 语音URL（如果是语音类型问题）
     * 图片、文档等附件文件统一通过 files 字段传入
     */
    private String voiceUrl;

    /**
     * 会话ID（标识同一次连续对话，前端生成并维护）
     */
    private String sessionId;

    /**
     * 附件文件列表（已上传至OSS，携带URL供AI使用）
     */
    private List<FileInput> files;

    @Data
    public static class FileInput {
        private String transferMethod;
        private String url;
        private String type;
    }
}
