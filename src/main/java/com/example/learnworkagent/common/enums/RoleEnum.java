package com.example.learnworkagent.common.enums;

import lombok.Getter;

/**
 * 角色枚举类
 */
@Getter
public enum RoleEnum {
    STUDENT("STUDENT", "学生"),
    COUNSELOR("COUNSELOR", "辅导员"),
    DEAN("DEAN", "院领导"),
    ADMIN("ADMIN", "部门领导");

    private final String code;
    private final String name;

    RoleEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据代码获取枚举
     */
    public static RoleEnum getByCode(String code) {
        for (RoleEnum role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 获取所有学工角色
     */
    public static RoleEnum[] getStaffRoles() {
        return new RoleEnum[]{COUNSELOR, DEAN, ADMIN};
    }

    /**
     * 获取所有学生角色
     */
    public static RoleEnum[] getStudentRoles() {
        return new RoleEnum[]{STUDENT};
    }

    /**
     * 判断是否为学工角色
     */
    public boolean isStaff() {
        return this != STUDENT;
    }

    /**
     * 判断是否为学生角色
     */
    public boolean isStudent() {
        return this == STUDENT;
    }
}