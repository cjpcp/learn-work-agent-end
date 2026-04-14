package com.example.learnworkagent.domain.consultation.entity;

import com.example.learnworkagent.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 人工转接记录实体.
 * <p>对应数据库中的human_transfer表，记录每次转人工的处理情况.</p>
 *
 * @author system
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Comment("人工转接记录表")
@Table(name = "human_transfer")
public class HumanTransfer extends BaseEntity {

    /**
     * 咨询问题ID（可能为空，当用户直接转人工时）
     */
    @Comment("咨询问题ID")
    @Column(name = "question_id")
    private Long questionId;

    /**
     * 问题类型
     */
    @Comment("问题类型")
    @Column(name = "question_type", length = 50)
    private String questionType;

    /**
     * 用户填写的转接问题描述
     */
    @Comment("用户填写的转接问题描述")
    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    /**
     * 附件URL列表，JSON格式存储
     */
    @Comment("附件URL列表")
    @Column(name = "file_urls", columnDefinition = "TEXT")
    private String fileUrls;

    /**
     * 用户ID
     */
    @Comment("用户ID")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 转接给的工作人员ID（辅导员或学工处人员）
     */
    @Comment("工作人员ID")
    @Column(name = "staff_id")
    private Long staffId;

    /**
     * 转接原因
     */
    @Comment("转接原因")
    @Column(name = "transfer_reason", length = 500)
    private String transferReason;

    /**
     * 转接方式（AUTO-自动识别, MANUAL-用户主动申请）
     */
    @Comment("转接方式")
    @Column(name = "transfer_type", nullable = false, length = 20)
    private String transferType;

    /**
     * 状态（PENDING-待处理, PROCESSING-处理中, COMPLETED-已完成）
     */
    @Comment("状态")
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    /**
     * 工作人员回复内容
     */
    @Comment("工作人员回复内容")
    @Column(name = "staff_reply", columnDefinition = "TEXT")
    private String staffReply;

    /**
     * 处理时间
     */
    @Comment("处理时间")
    @Column(name = "process_time")
    private java.time.LocalDateTime processTime;
}
