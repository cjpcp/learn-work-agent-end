package com.example.learnworkagent.domain.award.entity;

import com.example.learnworkagent.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Comment;
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
@Comment("奖助申请表")
@Table(name = "award_application")
public class AwardApplication extends BaseEntity {

    /**
     * 申请人ID
     */
    @Comment("申请人ID")
    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    /**
     * 申请类型（SCHOLARSHIP-奖学金, GRANT-助学金, SUBSIDY-困难补助）
     */
    @Comment("申请类型")
    @Column(name = "application_type", nullable = false, length = 20)
    private String applicationType;

    /**
     * 申请名称（如：国家励志奖学金）
     */
    @Comment("申请名称")
    @Column(name = "award_name", nullable = false, length = 200)
    private String awardName;

    /**
     * 申请金额
     */
    @Comment("申请金额")
    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * 申请理由
     */
    @Comment("申请理由")
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /**
     * 材料预审状态（PENDING-待预审, PASSED-通过, FAILED-未通过）
     */
    @Comment("材料预审状态")
    @Column(name = "material_status", nullable = false, length = 20)
    private String materialStatus = "PENDING";

    /**
     * 材料预审意见
     */
    @Comment("材料预审意见")
    @Column(name = "material_comment", length = 500)
    private String materialComment;

    /**
     * 材料预审时间
     */
    @Comment("材料预审时间")
    @Column(name = "material_review_time")
    private LocalDateTime materialReviewTime;

    /**
     * 审批人ID（学工处人员）
     */
    @Comment("审批人ID")
    @Column(name = "approver_id")
    private Long approverId;

    /**
     * 审批状态（PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝）
     */
    @Comment("审批状态")
    @Column(name = "approval_status", nullable = false, length = 20)
    private String approvalStatus = "PENDING";

    /**
     * 审批意见
     */
    @Comment("审批意见")
    @Column(name = "approval_comment", length = 500)
    private String approvalComment;

    /**
     * 审批时间
     */
    @Comment("审批时间")
    @Column(name = "approval_time")
    private LocalDateTime approvalTime;

    /**
     * 附件URL列表（JSON格式存储）
     */
    @Comment("附件URL列表")
    @Column(name = "attachment_urls", columnDefinition = "TEXT")
    private String attachmentUrls;

    /**
     * 姓名
     */
    @Comment("姓名")
    @Column(name = "student_name", length = 50)
    private String studentName;

    /**
     * 院系
     */
    @Comment("院系ID")
    @Column(name = "department_id")
    private Long departmentId;

    /**
     * 年级
     */
    @Comment("年级")
    @Column(name = "grade", length = 20)
    private String grade;

    /**
     * 班级
     */
    @Comment("班级")
    @Column(name = "class_name", length = 50)
    private String className;
}
