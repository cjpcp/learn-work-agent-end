package com.example.learnworkagent.domain.approval.entity;

import com.example.learnworkagent.common.BaseEntity;
import com.example.learnworkagent.common.enums.RoleEnum;
import jakarta.persistence.*;
import org.hibernate.annotations.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批步骤
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Comment("审批步骤表")
@Table(name = "approval_stage")
public class ApprovalStep extends BaseEntity {

    private static final String APPROVAL_TYPE_SINGLE = "SINGLE";
    private static final String APPROVAL_TYPE_MULTIPLE = "MULTIPLE";

    /**
     * 流程ID
     */
    @ManyToOne
    @Comment("流程ID")
    @JoinColumn(name = "process_id", nullable = false)
    private ApprovalProcess process;

    /**
     * 步骤名称
     */
    @Comment("步骤名称")
    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    /**
     * 名称（兼容旧字段）
     */
    @Comment("名称（兼容旧字段）")
    @Column(name = "name", length = 100)
    private String name;

    /**
     * 步骤顺序
     */
    @Comment("步骤顺序")
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    /**
     * 顺序索引（兼容旧字段）
     */
    @Comment("顺序索引（兼容旧字段）")
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    /**
     * 审批类型（SINGLE-单人审批, MULTIPLE-多人审批）
     */
    @Comment("审批类型（SINGLE-单人审批, MULTIPLE-多人审批）")
    @Column(name = "approval_type", nullable = false, length = 20)
    private String approvalType = "SINGLE";

    /**
     * 审批人角色（COUNSELOR-辅导员, COLLEGE_LEADER-院领导, DEPARTMENT_LEADER-部门领导）
     */
    @Comment("审批人角色（COUNSELOR-辅导员, COLLEGE_LEADER-院领导, DEPARTMENT_LEADER-部门领导）")
    @Column(name = "approver_role", nullable = false, length = 20)
    private String approverRole;

    /**
     * 审批人类型（兼容旧字段）
     */
    @Comment("审批人类型（兼容旧字段）")
    @Column(name = "approver_type", length = 20)
    private String approverType;

    /**
     * 是否必须通过
     */
    @Comment("是否必须通过")
    @Column(name = "must_pass", nullable = false)
    private Boolean mustPass = true;

    /**
     * 具体审批人ID
     */
    @Comment("具体审批人ID")
    @Column(name = "approver_user_id")
    private Long approverUserId;

    /**
     * 部门ID（用于部门领导审批步骤筛选）
     */
    @Comment("部门ID（用于部门领导审批步骤筛选）")
    @Column(name = "department_id")
    private Long departmentId;

    public boolean isSingleApproval() {
        return APPROVAL_TYPE_SINGLE.equals(approvalType);
    }

    public boolean isMultipleApproval() {
        return APPROVAL_TYPE_MULTIPLE.equals(approvalType);
    }

    public boolean requiresAllPass() {
        return Boolean.TRUE.equals(mustPass);
    }

    public boolean allowsRejectContinue() {
        return isMultipleApproval() && Boolean.FALSE.equals(mustPass);
    }

    public boolean isCounselorStep() {
        return RoleEnum.COUNSELOR.getCode().equals(approverRole);
    }

    public boolean isCollegeLeaderStep() {
        return RoleEnum.COLLEGE_LEADER.getCode().equals(approverRole);
    }

    public boolean isDepartmentLeaderStep() {
        return RoleEnum.DEPARTMENT_LEADER.getCode().equals(approverRole);
    }

    public boolean hasAssignedApprover() {
        return approverUserId != null;
    }
}
