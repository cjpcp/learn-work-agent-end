package com.example.learnworkagent.domain.approval.entity;

import com.example.learnworkagent.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审批任务
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
     * 审批状态（PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝）
     */
    @Comment("审批状态")
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

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
}
