package com.example.learnworkagent.common.enums;

/**
 * 用户状态枚举类
 */
public enum UserStatusEnum {
    ACTIVE("ACTIVE", "激活"),
    INACTIVE("INACTIVE", "禁用");

    private final String code;
    private final String name;

    UserStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据代码获取枚举
     */
    public static UserStatusEnum getByCode(String code) {
        for (UserStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}