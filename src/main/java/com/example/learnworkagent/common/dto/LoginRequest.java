package com.example.learnworkagent.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {

    @NotBlank(message = "学号/工号不能为空")
    private String studentNo;

    @NotBlank(message = "密码不能为空")
    private String password;
}
