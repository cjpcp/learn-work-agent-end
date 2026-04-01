package com.example.learnworkagent.common.enums;

import lombok.Getter;

/**
 * 通知业务类型枚举。
 */
@Getter
public enum NotificationBusinessTypeEnum {

    LEAVE("LEAVE", "请假"),
    AWARD("AWARD", "奖助申请");

    private final String code;
    private final String description;

    NotificationBusinessTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
