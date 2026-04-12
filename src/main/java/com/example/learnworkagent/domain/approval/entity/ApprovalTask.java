package com.example.learnworkagent.domain.approval.entity;

import com.example.learnworkagent.common.BaseEntity;
import com.example.learnworkagent.common.enums.ApprovalStatusEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 审批任务实体.
 * <p>对应数据库中的approval_task表，记录每个审批节点的任务信息.</p>
 *
 * @author system
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Comment("审批任务表")
@Table(name = "approval_task")
public class ApprovalTask extends BaseEntity {

    /**
     * 审批实例
     */
    @ManyToOne
    @Comment("审批实例ID")
    @JoinColumn(name = "instance_id", nullable = false)
    private ApprovalInstance instance;

    /**
     * 审批步骤
     */
    @ManyToOne
    @Comment("审批步骤ID")
    @JoinColumn(name = "step_id", nullable = false)
    private ApprovalStep step;

    /**
     * 审批人ID
     */
    @Comment("审批人ID")
    @Column(name = "approver_id", nullable = false)
    private Long approverId;

    /**
     * 审批状态（PENDING-待审批, PROCESSING-进行中, APPROVED-已批准, REJECTED-已拒绝）
     */
    @Comment("审批状态")
    @Column(name = "status", nullable = false, length = 20)
    private String status = ApprovalStatusEnum.PENDING.getCode();

    /**
     * 审批意见
     */
    @Comment("审批意见")
    @Column(name = "comment", length = 500)
    private String comment;

    /**
     * 审批时间
     */
    @Comment("审批时间")
    @Column(name = "approval_time")
    private LocalDateTime approvalTime;

    /**
     * 任务顺序
     */
    @Comment("任务顺序")
    @Column(name = "task_order")
    private Integer taskOrder;

    public boolean isPending() {
        return ApprovalStatusEnum.PENDING.getCode().equals(status);
    }

    public boolean isProcessing() {
        return ApprovalStatusEnum.PROCESSING.getCode().equals(status);
    }

    public boolean isApproved() {
        return ApprovalStatusEnum.APPROVED.getCode().equals(status);
    }

    public boolean isRejected() {
        return ApprovalStatusEnum.REJECTED.getCode().equals(status);
    }

    public void markPending() {
        this.status = ApprovalStatusEnum.PENDING.getCode();
        this.comment = null;
        this.approvalTime = null;
    }

    public void markProcessing() {
        this.status = ApprovalStatusEnum.PROCESSING.getCode();
        this.comment = null;
        this.approvalTime = null;
    }

    public void markApproved(String comment) {
        this.status = ApprovalStatusEnum.APPROVED.getCode();
        this.comment = comment;
        this.approvalTime = LocalDateTime.now();
    }

    public void markRejected(String comment) {
        this.status = ApprovalStatusEnum.REJECTED.getCode();
        this.comment = comment;
        this.approvalTime = LocalDateTime.now();
    }
}
