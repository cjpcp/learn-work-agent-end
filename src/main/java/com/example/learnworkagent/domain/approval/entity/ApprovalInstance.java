package com.example.learnworkagent.domain.approval.entity;

import com.example.learnworkagent.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审批实例
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Comment("审批实例表")
@Table(name = "approval_instance")
public class ApprovalInstance extends BaseEntity {

    /**
     * 业务类型（LEAVE-请假, AWARD-奖助）
     */
    @Comment("业务类型（LEAVE-请假, AWARD-奖助）")
    @Column(name = "business_type", nullable = false, length = 20)
    private String businessType;

    /**
     * 业务ID
     */
    @Comment("业务ID")
    @Column(name = "business_id", nullable = false)
    private Long businessId;

    /**
     * 流程ID
     */
    @ManyToOne
    @Comment("流程ID")
    @JoinColumn(name = "process_id", nullable = false)
    private ApprovalProcess process;

    /**
     * 当前步骤
     */
    @Comment("当前步骤")
    @Column(name = "current_step")
    private Integer currentStep;

    /**
     * 整体状态（PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝, CANCELLED-已取消）
     */
    @Comment("整体状态（PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝, CANCELLED-已取消）")
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    /**
     * 完成时间
     */
    @Comment("完成时间")
    @Column(name = "completed_time")
    private LocalDateTime completedTime;
}
