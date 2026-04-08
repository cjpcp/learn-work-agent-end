package com.example.learnworkagent.domain.consultation.entity;

import com.example.learnworkagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

/**
 * 人工转接配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "human_transfer_config")
@Comment("人工转接配置表")
public class HumanTransferConfig extends BaseEntity {

    @Comment("业务分类")
    @Column(name = "business_type", nullable = false, length = 50)
    private String businessType;

    @Comment("分配模式：USER/ROLE")
    @Column(name = "assign_mode", nullable = false, length = 20)
    private String assignMode;

    @Comment("目标角色ID")
    @Column(name = "role_id")
    private Long roleId;

    @Comment("目标用户ID列表，逗号分隔")
    @Column(name = "user_ids", length = 1000)
    private String userIds;

    @Comment("优先级")
    @Column(name = "priority", nullable = false)
    private Integer priority = 1;

    @Comment("是否启用")
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Comment("备注")
    @Column(name = "remark", length = 500)
    private String remark;
}
