package com.example.learnworkagent.common.exception;

import com.example.learnworkagent.common.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常。
 * 用于承载可预期的业务失败场景，便于统一异常处理。
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BusinessException(String message) {
        this(ResultCode.FAIL, message);
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        this(resultCode, resultCode.getMessage());
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
