package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.dto.LoginRequest;
import com.example.learnworkagent.common.dto.LoginResponse;
import com.example.learnworkagent.common.dto.RegisterRequest;
import com.example.learnworkagent.domain.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器.
 * <p>提供用户登录、注册等认证相关接口.</p>
 *
 * @author system
 * @see AuthService
 */
@Tag(name = "认证管理", description = "用户登录、注册等认证相关接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthService authService;

    /**
     * 用户登录接口.
     *
     * @param request 登录请求参数（用户名、密码）
     * @return 登录成功返回JWT Token和用户信息
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 用户注册接口.
     *
     * @param request 注册请求参数（用户名、密码、角色等）
     * @return 注册成功返回JWT Token和用户信息
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    /**
     * 检查用户名是否已存在.
     *
     * @param username 待检查的用户名
     * @return true表示用户名已存在，false表示可用
     */
    @Operation(summary = "检查用户名是否存在")
    @GetMapping("/check-username")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        return Result.success(authService.checkUsernameExists(username));
    }
}
