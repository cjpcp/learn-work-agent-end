package com.example.learnworkagent.common.enums;

import lombok.Getter;

@Getter
public enum MaterialStatusEnum {

    PENDING("PENDING", "待预审"),
    PASSED("PASSED", "通过"),
    FAILED("FAILED", "未通过");

    private final String code;
    private final String description;

    MaterialStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}