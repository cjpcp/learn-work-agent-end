package com.example.learnworkagent.domain.approval.entity;

import com.example.learnworkagent.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批流程定义
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Comment("审批流程表")
@Table(name = "approval_process")
public class ApprovalProcess extends BaseEntity {

    /**
     * 流程名称
     */
    @Comment("流程名称")
    @Column(name = "process_name", nullable = false, length = 100)
    private String processName;

    /**
     * 名称（兼容旧字段）
     */
    @Comment("名称（兼容旧字段）")
    @Column(name = "name", length = 100)
    private String name;

    /**
     * 流程类型（LEAVE-请假, AWARD-奖助）
     */
    @Comment("流程类型（LEAVE-请假, AWARD-奖助）")
    @Column(name = "process_type", nullable = false, length = 20)
    private String processType;

    /**
     * 类型（兼容旧字段）
     */
    @Comment("类型（兼容旧字段）")
    @Column(name = "type", length = 20)
    private String type;

    /**
     * 描述
     */
    @Comment("描述")
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 是否启用
     */
    @Comment("是否启用")
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * 版本号
     */
    @Comment("版本号")
    @Column(name = "version", nullable = false)
    private Integer version = 1;
}
