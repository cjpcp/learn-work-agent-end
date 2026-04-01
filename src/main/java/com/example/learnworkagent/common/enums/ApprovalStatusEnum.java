package com.example.learnworkagent.common.enums;

import lombok.Getter;

/**
 * 审批状态枚举。
 */
@Getter
public enum ApprovalStatusEnum {

    PENDING("PENDING", "待审批"),
    PROCESSING("PROCESSING", "处理中"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "未通过");

    private final String code;
    private final String description;

    ApprovalStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
