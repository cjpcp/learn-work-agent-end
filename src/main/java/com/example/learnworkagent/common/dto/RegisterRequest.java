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

    @NotBlank(message = "教师姓名不能为空")
    private String teacherName;

    @NotBlank(message = "联系电话不能为空")
    private String phone;

    @NotBlank(message = "学工号不能为空")
    private String cardNumber;
}
