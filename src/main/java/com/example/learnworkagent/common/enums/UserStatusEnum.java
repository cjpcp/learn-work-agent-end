package com.example.learnworkagent.common.enums;

import lombok.Getter;

/**
 * 用户状态枚举类
 */
@Getter
public enum UserStatusEnum {
    ACTIVE("ACTIVE", "激活"),
    INACTIVE("INACTIVE", "禁用");

    private final String code;
    private final String name;

    UserStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}