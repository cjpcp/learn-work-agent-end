package com.example.learnworkagent.domain.award.entity;

import com.example.learnworkagent.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 奖助申请实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "award_application")
public class AwardApplication extends BaseEntity {

    /**
     * 申请人ID
     */
    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    /**
     * 申请类型（SCHOLARSHIP-奖学金, GRANT-助学金, SUBSIDY-困难补助）
     */
    @Column(name = "application_type", nullable = false, length = 20)
    private String applicationType;

    /**
     * 申请名称（如：国家励志奖学金）
     */
    @Column(name = "award_name", nullable = false, length = 200)
    private String awardName;

    /**
     * 申请金额
     */
    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * 申请理由
     */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /**
     * 材料预审状态（PENDING-待预审, PASSED-通过, FAILED-未通过）
     */
    @Column(name = "material_status", nullable = false, length = 20)
    private String materialStatus = "PENDING";

    /**
     * 材料预审意见
     */
    @Column(name = "material_comment", length = 500)
    private String materialComment;

    /**
     * 材料预审时间
     */
    @Column(name = "material_review_time")
    private LocalDateTime materialReviewTime;

    /**
     * 审批人ID（学工处人员）
     */
    @Column(name = "approver_id")
    private Long approverId;

    /**
     * 审批状态（PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝）
     */
    @Column(name = "approval_status", nullable = false, length = 20)
    private String approvalStatus = "PENDING";

    /**
     * 审批意见
     */
    @Column(name = "approval_comment", length = 500)
    private String approvalComment;

    /**
     * 审批时间
     */
    @Column(name = "approval_time")
    private LocalDateTime approvalTime;

    /**
     * 附件URL列表（JSON格式存储）
     */
    @Column(name = "attachment_urls", columnDefinition = "TEXT")
    private String attachmentUrls;
}
