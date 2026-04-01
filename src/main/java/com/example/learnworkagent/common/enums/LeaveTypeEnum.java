package com.example.learnworkagent.common.enums;

import lombok.Getter;

/**
 * 请假类型枚举。
 */
@Getter
public enum LeaveTypeEnum {

    SICK("SICK", "病假"),
    PERSONAL("PERSONAL", "事假"),
    OFFICIAL("OFFICIAL", "公假");

    private final String code;
    private final String description;

    LeaveTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static String getDescriptionByCode(String code) {
        for (LeaveTypeEnum typeEnum : values()) {
            if (typeEnum.code.equals(code)) {
                return typeEnum.description;
            }
        }
        return code;
    }
}
