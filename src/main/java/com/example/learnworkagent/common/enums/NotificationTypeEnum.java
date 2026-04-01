package com.example.learnworkagent.common.enums;

import lombok.Getter;

/**
 * 通知类型枚举。
 */
@Getter
public enum NotificationTypeEnum {

    APPROVAL_RESULT("APPROVAL_RESULT", "审批结果通知"),
    SYSTEM("SYSTEM", "系统通知");

    private final String code;
    private final String description;

    NotificationTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
