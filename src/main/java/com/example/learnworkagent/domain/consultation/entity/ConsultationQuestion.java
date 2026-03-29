package com.example.learnworkagent.domain.consultation.entity;

import com.example.learnworkagent.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 咨询问题实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Comment("咨询问题表")
@Table(name = "consultation_question")
public class ConsultationQuestion extends BaseEntity {

    /**
     * 用户ID
     */
    @Comment("用户ID")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 问题内容（文本）
     */
    @Comment("问题内容")
    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    /**
     * 问题类型（TEXT-文本, VOICE-语音, IMAGE-图片�?
     */
    @Comment("问题类型")
    @Column(name = "question_type", nullable = false, length = 20)
    private String questionType;

    /**
     * 问题分类（AWARD-奖助勤贷, DORM-宿舍管理, DISCIPLINE-违纪申诉, MENTAL-心理健康, EMPLOYMENT-就业指导�?
     */
    @Comment("问题分类")
    @Column(name = "category", length = 50)
    private String category;

    /**
     * 语音URL（如果是语音类型问题）
     */
    @Comment("语音URL")
    @Column(name = "voice_url", length = 500)
    private String voiceUrl;

    /**
     * AI回答内容
     */
    @Comment("AI回答内容")
    @Column(name = "ai_answer", columnDefinition = "TEXT")
    private String aiAnswer;

    /**
     * 回答来源（AI-智能回答, HUMAN-人工回答
     */
    @Comment("回答来源")
    @Column(name = "answer_source", length = 20)
    private String answerSource;

    /**
     * 是否已转人工（true-已转人工, false-未转人工
     */
    @Comment("是否已转人工")
    @Column(name = "transferred_to_human", nullable = false)
    private Boolean transferredToHuman = false;

    /**
     * 转人工原因
     */
    @Comment("转人工原因")
    @Column(name = "transfer_reason", length = 500)
    private String transferReason;

    /**
     * 状态（PENDING-待回�? ANSWERED-已回�? TRANSFERRED-已转人工�?
     */
    @Comment("状态")
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    /**
     * 满意度评分（1-5�?
     */
    @Comment("满意度评")
    @Column(name = "satisfaction_score")
    private Integer satisfactionScore;

    /**
     * 会话ID（标识同一次连续对话，用于上下文关联）
     */
    @Comment("会话ID")
    @Column(name = "session_id", length = 64)
    private String sessionId;

    /**
     * 附件文件URL列表（JSON数组，存储已上传至OSS的文件URL�?
     */
    @Comment("附件文件URL列表(JSON)")
    @Column(name = "file_urls", columnDefinition = "TEXT")
    private String fileUrls;
}
