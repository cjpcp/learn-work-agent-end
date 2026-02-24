package com.example.learnworkagent.domain.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 咨询请求DTO
 */
@Data
public class ConsultationRequest {

    @NotBlank(message = "问题内容不能为空")
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
     * 图片URL（如果是图片类型问题）
     */
    private String imageUrl;

    /**
     * 语音URL（如果是语音类型问题）
     */
    private String voiceUrl;
}
