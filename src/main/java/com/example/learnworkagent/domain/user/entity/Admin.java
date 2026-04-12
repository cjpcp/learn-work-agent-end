package com.example.learnworkagent.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 管理员账户实体.
 * <p>对应数据库中的admin表，存储系统用户账户信息.</p>
 *
 * @author system
 */
@Data
@Entity
@Table(name = "admin")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键")
    @Column(name = "id", nullable = false)
    private Long id;

    @Comment("用户名")
    @Column(name = "username", nullable = false, length = 30)
    private String username;

    @Comment("昵称")
    @Column(name = "nick", nullable = false, length = 30)
    private String nick;

    @Comment("密码")
    @Column(name = "password", nullable = false)
    private String password;

    @Comment("角色id")
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "login_time")
    private LocalDateTime loginTime;

    @Comment("状态")
    @Column(name = "status", nullable = false)
    private Integer status;

    @Comment("教师id")
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    public boolean isEnabled() {
        return Integer.valueOf(1).equals(status);
    }
}
