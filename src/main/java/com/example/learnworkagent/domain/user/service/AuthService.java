package com.example.learnworkagent.domain.user.service;

import com.example.learnworkagent.common.dto.LoginRequest;
import com.example.learnworkagent.common.dto.LoginResponse;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.util.JwtUtil;
import com.example.learnworkagent.domain.user.entity.User;
import com.example.learnworkagent.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    /**
     * 登录
     *
     * @param request 用户名和密码
     * @return token和用户具体信息
     */
    public LoginResponse login(LoginRequest request) {

        //根据用户名查找用户，如果不存在则抛出异常
        User user = userRepository.findByUsernameAndDeletedFalse(request.getUsername())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误"));

        //非活跃用户判断，如果是非活跃用户抛出异常
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户已被禁用");
        }

        //验证用户密码，不正确抛出异常
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        //生成token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        //构建登录响应并返回
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole(user.getRole());

        return response;
    }

    /**
     * 注册
     */
    @Transactional
    public User register(String username, String password, String realName,
                         String studentNo, String phone, String email, String role) {

        //根据用户名进行查询，如果已存在则进行报错
        if (userRepository.findByUsernameAndDeletedFalse(username).isPresent()) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        //构建用户并保存
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setStudentNo(studentNo);
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole(role != null ? role : "STUDENT");
        user.setStatus("ACTIVE");

        return userRepository.save(user);
    }
}
