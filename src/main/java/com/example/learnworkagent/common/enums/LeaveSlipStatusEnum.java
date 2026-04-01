package com.example.learnworkagent.common.enums;

import lombok.Getter;

/**
 * 请假条生成状态枚举。
 */
@Getter
public enum LeaveSlipStatusEnum {

    NOT_GENERATED("NOT_GENERATED", "未生成"),
    GENERATED("GENERATED", "已生成");

    private final String code;
    private final String description;

    LeaveSlipStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
