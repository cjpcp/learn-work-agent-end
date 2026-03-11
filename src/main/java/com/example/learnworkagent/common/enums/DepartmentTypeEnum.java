package com.example.learnworkagent.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 部门类型枚举
 */
@Getter
@AllArgsConstructor
public enum DepartmentTypeEnum {

    /**
     * 学院
     */
    COLLEGE("COLLEGE", "学院"),

    /**
     * 部门
     */
    DEPARTMENT("DEPARTMENT", "部门");

    private final String code;
    private final String name;
}
