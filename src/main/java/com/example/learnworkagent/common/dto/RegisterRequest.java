package com.example.learnworkagent.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 注册请求DTO
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nick;

    @NotNull(message = "角色不能为空")
    private Long roleId;

    @NotNull(message = "请选择是否为教师")
    private Boolean teacher;

    private String teacherName;

    private String phone;

    private String cardNumber;
}
