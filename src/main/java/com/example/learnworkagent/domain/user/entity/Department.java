package com.example.learnworkagent.domain.user.entity;

import com.example.learnworkagent.common.BaseEntity;
import com.example.learnworkagent.common.enums.DepartmentTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Comment("部门表")
@Table(name = "sys_department")
public class Department extends BaseEntity {

    /**
     * 部门编码
     */
    @Comment("部门编码")
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * 部门名称
     */
    @Comment("部门名称")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 部门描述
     */
    @Comment("部门描述")
    @Column(name = "description")
    private String description;

    /**
     * 排序号
     */
    @Comment("排序号")
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 是否启用
     */
    @Comment("是否启用")
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * 类型：学院或部门
     */
    @Comment("类型：学院或部门")
    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DepartmentTypeEnum type;
}
