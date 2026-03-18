package com.example.learnworkagent.common.enums;

import lombok.Getter;

/**
 * 角色枚举类
 */
@Getter
public enum RoleEnum {
    ADMIN("ADMIN", "管理员"),
    STUDENT("STUDENT", "学生"),
    COUNSELOR("COUNSELOR", "辅导员"),
    COLLEGE_LEADER("COLLEGE_LEADER", "院领导"),
    DEPARTMENT_LEADER("DEPARTMENT_LEADER", "部门领导");


    private final String code;
    private final String name;

    RoleEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }


    /**
     * 获取所有学工角色
     */
    public static RoleEnum[] getStaffRoles() {
        return new RoleEnum[]{COUNSELOR, COLLEGE_LEADER, DEPARTMENT_LEADER};
    }

}