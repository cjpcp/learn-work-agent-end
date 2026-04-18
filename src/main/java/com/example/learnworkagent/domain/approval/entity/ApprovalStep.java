package com.example.learnworkagent.domain.approval.entity;

import com.example.learnworkagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

/**
 * 审批步骤实体.
 * <p>对应数据库中的approval_stage表，定义审批流程中的单个步骤.</p>
 *
 * @author system
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Comment("审批步骤表")
@Table(name = "approval_stage")
public class ApprovalStep extends BaseEntity {

    private static final String APPROVAL_TYPE_SINGLE = "SINGLE";
    private static final String APPROVAL_TYPE_MULTIPLE = "MULTIPLE";

    @ManyToOne
    @Comment("流程ID")
    @JoinColumn(name = "process_id", nullable = false)
    private ApprovalProcess process;

    @Comment("步骤名称")
    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Comment("步骤顺序")
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Comment("审批类型（SINGLE-单人审批, MULTIPLE-多人审批）")
    @Column(name = "approval_type", nullable = false, length = 20)
    private String approvalType = APPROVAL_TYPE_SINGLE;

    @Comment("审批人角色（COUNSELOR-辅导员, COLLEGE_LEADER-院领导, DEPARTMENT_LEADER-部门领导）")
    @Column(name = "approver_role", nullable = false, length = 20)
    private String approverRole;

    @Comment("是否必须通过")
    @Column(name = "must_pass", nullable = false)
    private Boolean mustPass = true;

    @Comment("具体审批人ID")
    @Column(name = "approver_user_id")
    private Long approverUserId;

    @Comment("分配模式（USER-指定用户, ROLE-按角色池分配）")
    @Column(name = "assign_mode", length = 20)
    private String assignMode = "USER";

    @Comment("目标角色ID（按角色池分配时使用）")
    @Column(name = "role_id", length = 50)
    private String roleId;

    public boolean isMultipleApproval() {
        return APPROVAL_TYPE_MULTIPLE.equals(approvalType);
    }

    public boolean requiresAllPass() {
        return Boolean.TRUE.equals(mustPass);
    }

    public boolean allowsRejectContinue() {
        return isMultipleApproval() && Boolean.FALSE.equals(mustPass);
    }

    public boolean hasAssignedApprover() {
        return approverUserId != null;
    }
}
