package com.example.learnworkagent.common.exception;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
 * 全局异常处理器。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常。
     *
     * @param exception 业务异常
     * @return 统一响应结果
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        log.error("业务异常: {}", exception.getMessage(), exception);
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = extractValidationMessage(exception.getBindingResult().getFieldErrors());
        log.error("参数校验异常: {}", message);
        return Result.fail(ResultCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 处理参数绑定异常。
     *
     * @param exception 参数绑定异常
     * @param request 当前请求
     * @return 统一响应结果或 SSE 响应
     */
    @ExceptionHandler(BindException.class)
    public Object handleBindException(BindException exception, NativeWebRequest request) {
        String message = extractValidationMessage(exception.getBindingResult().getFieldErrors());
        log.error("参数绑定异常: {}", message);
        return buildResponse(request, HttpStatus.BAD_REQUEST, Result.fail(ResultCode.PARAM_INVALID.getCode(), message));
    }

    /**
     * 处理访问拒绝异常。
     *
     * @param exception 访问拒绝异常
     * @param request 当前请求
     * @return 统一响应结果或 SSE 响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDeniedException(AccessDeniedException exception, NativeWebRequest request) {
        log.error("访问拒绝异常: {}", exception.getMessage());
        return buildResponse(request, HttpStatus.FORBIDDEN, Result.fail(ResultCode.FORBIDDEN.getCode(), "未授权访问"));
    }

    /**
     * 处理未找到处理器异常。
     *
     * @param exception 未找到处理器异常
     * @param request 当前请求
     * @return 统一响应结果或 SSE 响应
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Object handleNoHandlerFoundException(NoHandlerFoundException exception, NativeWebRequest request) {
        log.error("未找到处理器: {}", exception.getRequestURL());
        return buildResponse(request, HttpStatus.NOT_FOUND, Result.fail(ResultCode.PARAM_ERROR.getCode(), "请求的资源不存在"));
    }

    /**
     * 处理其他未捕获异常。
     *
     * @param exception 系统异常
     * @param request 当前请求
     * @return 统一响应结果或 SSE 响应
     */
    @ExceptionHandler(Exception.class)
    public Object handleException(Exception exception, NativeWebRequest request) {
        log.error("系统异常", exception);
        return buildResponse(request, HttpStatus.INTERNAL_SERVER_ERROR, Result.fail(ResultCode.SYSTEM_ERROR));
    }

    private String extractValidationMessage(Iterable<FieldError> fieldErrors) {
        return java.util.stream.StreamSupport.stream(fieldErrors.spliterator(), false)
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
    }

    private Object buildResponse(NativeWebRequest request, HttpStatus httpStatus, Result<Void> result) {
        if (isSseRequest(request)) {
            return ResponseEntity.status(httpStatus).body(result);
        }
        return result;
    }

    private boolean isSseRequest(NativeWebRequest request) {
        String acceptHeader = request.getHeader("Accept");
        return acceptHeader != null && acceptHeader.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
