package com.example.learnworkagent.common.exception;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("参数校验异常: {}", message);
        return Result.fail(ResultCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Object handleBindException(BindException e, NativeWebRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("参数绑定异常: {}", message);

        // 检查请求是否接受text/event-stream
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains(MediaType.TEXT_EVENT_STREAM_VALUE)) {
            // 对于SSE请求，返回错误信息而不中断流
            return ResponseEntity.status(400).body(Result.fail(ResultCode.PARAM_INVALID.getCode(), message));
        }

        return Result.fail(ResultCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 处理访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDeniedException(AccessDeniedException e, NativeWebRequest request) {
        log.error("访问拒绝异常: {}", e.getMessage());

        // 检查请求是否接受text/event-stream
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains(MediaType.TEXT_EVENT_STREAM_VALUE)) {
            // 对于SSE请求，返回错误信息
            return ResponseEntity.status(403).body(Result.fail(ResultCode.FORBIDDEN.getCode(), "未授权访问"));
        }

        return Result.fail(ResultCode.FORBIDDEN.getCode(), "未授权访问");
    }

    /**
     * 处理未找到处理器异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Object handleNoHandlerFoundException(NoHandlerFoundException e, NativeWebRequest request) {
        log.error("未找到处理器: {}", e.getRequestURL());

        // 检查请求是否接受text/event-stream
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains(MediaType.TEXT_EVENT_STREAM_VALUE)) {
            // 对于SSE请求，返回错误信息
            return ResponseEntity.status(404).body(Result.fail(ResultCode.PARAM_ERROR.getCode(), "请求的资源不存在"));
        }

        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "请求的资源不存在");
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e, NativeWebRequest request) {
        log.error("系统异常: ", e);

        // 检查请求是否接受text/event-stream
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains(MediaType.TEXT_EVENT_STREAM_VALUE)) {
            // 对于SSE请求，返回错误信息
            return ResponseEntity.status(500).body(Result.fail(ResultCode.SYSTEM_ERROR));
        }

        return Result.fail(ResultCode.SYSTEM_ERROR);
    }
}
