package com.example.learnworkagent.domain.user.entity;

import com.example.learnworkagent.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_user")
public class User extends BaseEntity {

    /**
     * 用户名
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 密码（加密后）
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * 真实姓名
     */
    @Column(name = "real_name", length = 50)
    private String realName;

    /**
     * 学号/工号
     */
    @Column(name = "student_no", length = 50)
    private String studentNo;

    /**
     * 手机号
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 邮箱
     */
    @Column(name = "email", length = 100)
    private String email;

    /**
     * 角色（STUDENT-学生, COUNSELOR-辅导员, ADMIN-管理员）
     */
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    /**
     * 状态（ACTIVE-激活, INACTIVE-禁用）
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    /**
     * 院系（学生角色使用）
     */
    @Column(name = "department", length = 100)
    private String department;

    /**
     * 年级（学生角色使用）
     */
    @Column(name = "grade", length = 20)
    private String grade;

    /**
     * 班级（学生角色使用）
     */
    @Column(name = "class_name", length = 50)
    private String className;

    /**
     * 所属部门（辅导员/学工角色使用）
     */
    @Column(name = "work_department", length = 100)
    private String workDepartment;

    /**
     * 职位（辅导员/学工角色使用）
     */
    @Column(name = "position", length = 50)
    private String position;
}
