package com.example.learnworkagent.domain.user.entity;

import com.example.learnworkagent.common.BaseEntity;
import com.example.learnworkagent.common.enums.UserStatusEnum;
import jakarta.persistence.*;
import org.hibernate.annotations.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Comment("用户表")
@Table(name = "sys_user")
public class User extends BaseEntity {

    /**
     * 用户名
     */
    @Comment("用户名")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 密码（加密后）
     */
    @Comment("密码（加密后）")
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * 真实姓名
     */
    @Comment("真实姓名")
    @Column(name = "real_name", length = 50)
    private String realName;

    /**
     * 学号/工号
     */
    @Comment("学号/工号")
    @Column(name = "student_no", length = 50)
    private String studentNo;

    /**
     * 手机号
     */
    @Comment("手机号")
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 邮箱
     */
    @Comment("邮箱")
    @Column(name = "email", length = 100)
    private String email;

    /**
     * 微信 OpenID
     */
    @Comment("微信 OpenID")
    @Column(name = "wechat_open_id", length = 100)
    private String wechatOpenId;

    /**
     * 企业微信 UserId
     */
    @Comment("企业微信 UserId")
    @Column(name = "wework_user_id", length = 100)
    private String weworkUserId;

    /**
     * 角色
     */
    @Comment("角色")
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    /**
     * 状态
     */
    @Comment("状态")
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserStatusEnum status = UserStatusEnum.ACTIVE;

    /**
     * 部门ID（学生，辅导员指学院，领导指部门）
     */
    @Comment("部门ID（学生，辅导员指学院，领导指部门）")
    @Column(name = "department_id")
    private Long departmentId;

    /**
     * 年级（学生，辅导员角色使用）
     */
    @Comment("年级（学生角色使用）")
    @Column(name = "grade", length = 20)
    private String grade;

    /**
     * 班级（学生角色使用）
     */
    @Comment("班级（学生角色使用）")
    @Column(name = "class_name", length = 50)
    private String className;

}
