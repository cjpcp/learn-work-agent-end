package com.example.learnworkagent.domain.leave.entity;

import com.example.learnworkagent.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假申请实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "leave_application")
public class LeaveApplication extends BaseEntity {

    /**
     * 申请人ID
     */
    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    /**
     * 请假类型（SICK-病假, PERSONAL-事假, OFFICIAL-公假）
     */
    @Column(name = "leave_type", nullable = false, length = 20)
    private String leaveType;

    /**
     * 请假开始日期
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * 请假结束日期
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * 请假天数
     */
    @Column(name = "days", nullable = false)
    private Integer days;

    /**
     * 请假原因
     */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /**
     * 附件URL（如病假条等）
     */
    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    /**
     * 审批人ID（辅导员）
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
     * 请假条生成状态（GENERATED-已生成, NOT_GENERATED-未生成）
     */
    @Column(name = "leave_slip_status", length = 20)
    private String leaveSlipStatus = "NOT_GENERATED";

    /**
     * 请假条URL
     */
    @Column(name = "leave_slip_url", length = 500)
    private String leaveSlipUrl;

    /**
     * 是否已销假（true-已销假, false-未销假）
     */
    @Column(name = "cancelled", nullable = false)
    private Boolean cancelled = false;

    /**
     * 销假时间
     */
    @Column(name = "cancel_time")
    private LocalDateTime cancelTime;
}
