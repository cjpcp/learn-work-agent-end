package com.example.learnworkagent.domain.consultation.entity;

import com.example.learnworkagent.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 咨询问题实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "consultation_question")
public class ConsultationQuestion extends BaseEntity {

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 问题内容（文本）
     */
    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    /**
     * 问题类型（TEXT-文本, VOICE-语音, IMAGE-图片）
     */
    @Column(name = "question_type", nullable = false, length = 20)
    private String questionType;

    /**
     * 问题分类（AWARD-奖助勤贷, DORM-宿舍管理, DISCIPLINE-违纪申诉, MENTAL-心理健康, EMPLOYMENT-就业指导）
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * 图片URL（如果是图片类型问题）
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * 语音URL（如果是语音类型问题）
     */
    @Column(name = "voice_url", length = 500)
    private String voiceUrl;

    /**
     * AI回答内容
     */
    @Column(name = "ai_answer", columnDefinition = "TEXT")
    private String aiAnswer;

    /**
     * 回答来源（AI-智能回答, HUMAN-人工回答）
     */
    @Column(name = "answer_source", length = 20)
    private String answerSource;

    /**
     * 是否已转人工（true-已转人工, false-未转人工）
     */
    @Column(name = "transferred_to_human", nullable = false)
    private Boolean transferredToHuman = false;

    /**
     * 转人工原因
     */
    @Column(name = "transfer_reason", length = 500)
    private String transferReason;

    /**
     * 状态（PENDING-待回答, ANSWERED-已回答, TRANSFERRED-已转人工）
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    /**
     * 满意度评分（1-5）
     */
    @Column(name = "satisfaction_score")
    private Integer satisfactionScore;
}
