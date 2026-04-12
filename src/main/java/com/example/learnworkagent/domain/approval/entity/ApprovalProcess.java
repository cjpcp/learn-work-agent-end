package com.example.learnworkagent.domain.approval.entity;

import com.example.learnworkagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

/**
 * 审批流程实体.
 * <p>对应数据库中的approval_process表，定义审批流程的模板信息.</p>
 *
 * @author system
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Comment("审批流程表")
@Table(name = "approval_process")
public class ApprovalProcess extends BaseEntity {

    private static final int INITIAL_VERSION = 1;

    @Comment("流程名称")
    @Column(name = "process_name", nullable = false, length = 100)
    private String processName;

    @Comment("流程类型（LEAVE-请假, AWARD-奖助）")
    @Column(name = "process_type", nullable = false, length = 20)
    private String processType;

    @Comment("描述")
    @Column(name = "description", length = 500)
    private String description;

    @Comment("是否启用")
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Comment("版本号")
    @Column(name = "version", nullable = false)
    private Integer version = INITIAL_VERSION;

    public void enable() {
        this.enabled = Boolean.TRUE;
    }

    public void disable() {
        this.enabled = Boolean.FALSE;
    }
}
