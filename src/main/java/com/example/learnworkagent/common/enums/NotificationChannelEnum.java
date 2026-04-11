package com.example.learnworkagent.common.enums;

import lombok.Getter;

@Getter
public enum NotificationChannelEnum {

    SITE("SITE", "站内信"),
    EMAIL("EMAIL", "邮件"),
    ALL("ALL", "全部渠道");

    private final String code;
    private final String description;

    NotificationChannelEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
