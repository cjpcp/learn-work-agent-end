package com.example.learnworkagent.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * Token
     */
    private String token;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 角色
     */
    private String role;

    /**
     * 院系（学生角色使用）
     */
    private String department;

    /**
     * 年级（学生角色使用）
     */
    private String grade;

    /**
     * 班级（学生角色使用）
     */
    private String className;

    /**
     * 所属部门（辅导员/学工角色使用）
     */
    private String workDepartment;

    /**
     * 职位（辅导员/学工角色使用）
     */
    private String position;
}
