package com.example.learnworkagent.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 角色实体.
 * <p>对应数据库中的role表，定义系统用户角色及权限关联.</p>
 *
 * @author system
 */
@Data
@Entity
@Table(name = "role")
public class Role {

    @Id
    @Comment("主键")
    @Column(name = "id", nullable = false)
    private Long id;

    @Comment("角色名称")
    @Column(name = "role_name", nullable = false, length = 20)
    private String roleName;

    @Comment("权限id")
    @Column(name = "power_id")
    private String powerId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "page_path")
    private String pagePath;
}
