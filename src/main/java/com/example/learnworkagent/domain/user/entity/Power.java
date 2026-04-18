package com.example.learnworkagent.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 权限实体.
 * <p>对应数据库中的power表，定义系统权限信息.</p>
 *
 * @author system
 */
@Data
@Entity
@Table(name = "power")
public class Power {

    @Id
    @Comment("主键")
    @Column(name = "id", nullable = false)
    private Long id;

    @Comment("权限名称")
    @Column(name = "power_name", nullable = false)
    private String powerName;

    @Comment("权限地址")
    @Column(name = "power_url", nullable = false)
    private String powerUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Comment("父级id")
    @Column(name = "pid", nullable = false)
    private Integer pid;

    @Comment("等级")
    @Column(name = "level", nullable = false)
    private Integer level;
}
