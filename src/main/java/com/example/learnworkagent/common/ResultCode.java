package com.example.learnworkagent.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    // 认证授权相关 1000-1099
    UNAUTHORIZED(1001, "未登录或登录已过期"),
    FORBIDDEN(1002, "没有权限访问"),
    TOKEN_INVALID(1003, "Token无效"),
    TOKEN_EXPIRED(1004, "Token已过期"),

    // 参数校验相关 1100-1199
    PARAM_ERROR(1100, "参数错误"),
    PARAM_MISSING(1101, "参数缺失"),
    PARAM_INVALID(1102, "参数无效"),

    // 业务相关 2000-2999
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_ALREADY_EXISTS(2002, "用户已存在"),
    LEAVE_APPLICATION_NOT_FOUND(2003, "请假申请不存在"),
    AWARD_APPLICATION_NOT_FOUND(2004, "奖助申请不存在"),

    // AI服务相关 3000-3099
    AI_SERVICE_ERROR(3001, "AI服务调用失败"),
    AI_SERVICE_TIMEOUT(3002, "AI服务调用超时"),
    AI_SERVICE_RATE_LIMIT(3003, "AI服务调用频率超限"),

    // 系统相关 9000-9999
    SYSTEM_ERROR(9000, "系统错误"),
    DATABASE_ERROR(9001, "数据库错误"),
    CACHE_ERROR(9002, "缓存错误"),
    MQ_ERROR(9003, "消息队列错误");

    private final Integer code;
    private final String message;
}
