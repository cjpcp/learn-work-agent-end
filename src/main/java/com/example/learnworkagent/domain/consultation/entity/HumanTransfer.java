package com.example.learnworkagent.domain.consultation.entity;

import com.example.learnworkagent.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 人工转接记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "human_transfer")
public class HumanTransfer extends BaseEntity {

    /**
     * 咨询问题ID
     */
    @Column(name = "question_id", nullable = false)
    private Long questionId;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 转接给的工作人员ID（辅导员或学工处人员）
     */
    @Column(name = "staff_id")
    private Long staffId;

    /**
     * 转接原因
     */
    @Column(name = "transfer_reason", length = 500)
    private String transferReason;

    /**
     * 转接方式（AUTO-自动识别, MANUAL-用户主动申请）
     */
    @Column(name = "transfer_type", nullable = false, length = 20)
    private String transferType;

    /**
     * 状态（PENDING-待处理, PROCESSING-处理中, COMPLETED-已完成）
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    /**
     * 工作人员回复内容
     */
    @Column(name = "staff_reply", columnDefinition = "TEXT")
    private String staffReply;

    /**
     * 处理时间
     */
    @Column(name = "process_time")
    private java.time.LocalDateTime processTime;
}
