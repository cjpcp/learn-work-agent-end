package com.example.learnworkagent.domain.leave.entity;

import com.example.learnworkagent.common.BaseEntity;
import com.example.learnworkagent.common.enums.ApprovalStatusEnum;
import com.example.learnworkagent.common.enums.LeaveSlipStatusEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假申请实体.
 * <p>对应数据库中的leave_application表，存储学生请假申请信息.</p>
 *
 * @author system
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Comment("请假申请表")
@Table(name = "leave_application")
public class LeaveApplication extends BaseEntity {

    /**
     * 申请人ID。
     */
    @Comment("申请人ID")
    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    /**
     * 请假类型。
     */
    @Comment("请假类型")
    @Column(name = "leave_type", nullable = false, length = 20)
    private String leaveType;

    /**
     * 请假开始日期。
     */
    @Comment("请假开始日期")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * 请假结束日期。
     */
    @Comment("请假结束日期")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * 请假天数。
     */
    @Comment("请假天数")
    @Column(name = "days", nullable = false)
    private Integer days;

    /**
     * 请假原因。
     */
    @Comment("请假原因")
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /**
     * 附件地址。
     */
    @Comment("附件URL")
    @Column(name = "attachment_url", columnDefinition = "TEXT")
    private String attachmentUrl;

    /**
     * 审批人ID。
     */
    @Comment("审批人ID")
    @Column(name = "approver_id")
    private Long approverId;

    /**
     * 审批状态。
     */
    @Comment("审批状态")
    @Column(name = "approval_status", nullable = false, length = 20)
    private String approvalStatus = ApprovalStatusEnum.PENDING.getCode();

    /**
     * 审批意见。
     */
    @Comment("审批意见")
    @Column(name = "approval_comment", length = 500)
    private String approvalComment;

    /**
     * 审批时间。
     */
    @Comment("审批时间")
    @Column(name = "approval_time")
    private LocalDateTime approvalTime;

    /**
     * 请假条生成状态。
     */
    @Comment("请假条生成状态")
    @Column(name = "leave_slip_status", length = 20)
    private String leaveSlipStatus = LeaveSlipStatusEnum.NOT_GENERATED.getCode();

    /**
     * 是否已销假。
     */
    @Comment("是否已销假")
    @Column(name = "cancelled", nullable = false)
    private Boolean cancelled = false;

    /**
     * 销假时间。
     */
    @Comment("销假时间")
    @Column(name = "cancel_time")
    private LocalDateTime cancelTime;

    /**
     * 是否已申请销假。
     */
    @Comment("是否已申请销假")
    @Column(name = "cancel_requested", nullable = false)
    private Boolean cancelRequested = false;

    /**
     * 销假审批状态。
     */
    @Comment("销假审批状态")
    @Column(name = "cancel_approval_status", length = 20)
    private String cancelApprovalStatus;

    /**
     * 销假审批意见。
     */
    @Comment("销假审批意见")
    @Column(name = "cancel_approval_comment", length = 500)
    private String cancelApprovalComment;

    /**
     * 销假审批时间。
     */
    @Comment("销假审批时间")
    @Column(name = "cancel_approval_time")
    private LocalDateTime cancelApprovalTime;

    /**
     * 学生姓名。
     */
    @Comment("姓名")
    @Column(name = "student_name", length = 50)
    private String studentName;

    /**
     * 院系名称。
     */
    @Comment("院系名称")
    @Column(name = "department_id", length = 50)
    private String departmentName;

    /**
     * 年级。
     */
    @Comment("年级")
    @Column(name = "grade", length = 20)
    private String grade;

    /**
     * 班级。
     */
    @Comment("班级")
    @Column(name = "class_name", length = 50)
    private String className;

    /**
     * 标记为已销假。
     */
    public void markCancelled() {
        this.cancelled = Boolean.TRUE;
        this.cancelTime = LocalDateTime.now();
    }

    /**
     * 判断当前是否已审批通过。
     *
     * @return 是否已通过
     */
    public boolean isApproved() {
        return ApprovalStatusEnum.APPROVED.getCode().equals(this.approvalStatus);
    }

}
