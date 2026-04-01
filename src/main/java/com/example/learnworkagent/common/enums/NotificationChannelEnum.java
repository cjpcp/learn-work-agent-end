package com.example.learnworkagent.common.enums;

import lombok.Getter;

/**
 * 通知渠道枚举。
 */
@Getter
public enum NotificationChannelEnum {

    SITE("SITE", "站内信"),
    EMAIL("EMAIL", "邮件"),
    SMS("SMS", "短信"),
    WECHAT_MP("WECHAT_MP", "微信公众号"),
    WEWORK("WEWORK", "企业微信"),
    ALL("ALL", "全部渠道");

    private final String code;
    private final String description;

    NotificationChannelEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
