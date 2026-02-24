package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.dto.LoginRequest;
import com.example.learnworkagent.common.dto.LoginResponse;
import com.example.learnworkagent.domain.user.entity.User;
import com.example.learnworkagent.domain.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Tag(name = "认证管理", description = "用户登录、注册等认证相关接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthService authService;

    /**
     * 用户登录
     *
     * @param request 用户名，密码封装
     * @return token，userId等信息
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    /**
     * 用户注册
     *
     * @param user 用户名，密码，真实姓名，学号，手机号，邮箱，角色等用户信息
     * @return 注册成功的用户信息
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
        User registered = authService.register(
                user.getUsername(),
                user.getPassword(),
                user.getRealName(),
                user.getStudentNo(),
                user.getPhone(),
                user.getEmail(),
                user.getRole()
        );
        return Result.success(registered);
    }
}
