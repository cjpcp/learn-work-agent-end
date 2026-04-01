package com.example.learnworkagent.common.enums;

import lombok.Getter;

/**
 * 奖助申请类型枚举。
 */
@Getter
public enum AwardApplicationTypeEnum {

    SCHOLARSHIP("SCHOLARSHIP", "奖学金申请"),
    GRANT("GRANT", "助学金申请"),
    SUBSIDY("SUBSIDY", "困难补助申请");

    private final String code;
    private final String description;

    AwardApplicationTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
