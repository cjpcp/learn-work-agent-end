package com.example.learnworkagent.domain.user.service;

import com.example.learnworkagent.common.dto.LoginRequest;
import com.example.learnworkagent.common.dto.LoginResponse;
import com.example.learnworkagent.common.enums.RoleEnum;
import com.example.learnworkagent.common.enums.UserStatusEnum;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.util.JwtUtil;
import com.example.learnworkagent.common.util.RsaUtil;
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
    private final RsaUtil rsaUtil;


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
        if (user.getStatus() != UserStatusEnum.ACTIVE) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户已被禁用");
        }

        // 解密密码（前端RSA加密传输）
        String decryptedPassword = rsaUtil.decrypt(request.getPassword());

        //验证用户密码，不正确抛出异常
        if (!passwordEncoder.matches(decryptedPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        //生成token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().getCode());

        //构建登录响应并返回
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole(user.getRole().getCode());
        response.setDepartment(user.getDepartment());
        response.setGrade(user.getGrade());
        response.setClassName(user.getClassName());
        response.setWorkDepartment(user.getWorkDepartment());
        response.setPosition(user.getPosition());

        return response;
    }

    /**
     * 注册
     */
    @Transactional
    public User register(String username, String password, String realName,
                         String studentNo, String phone, String email, String role,
                         String department, String grade, String className,
                         String workDepartment, Long workDepartmentId, String position) {

        if (userRepository.findByUsernameAndDeletedFalse(username).isPresent()) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        if (studentNo != null && !studentNo.trim().isEmpty() && userRepository.findByStudentNoAndDeletedFalse(studentNo).isPresent()) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "学号/工号已存在");
        }

        // 解密密码（前端RSA加密传输）
        String decryptedPassword = rsaUtil.decrypt(password);

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(decryptedPassword));
        user.setRealName(realName);
        user.setStudentNo(studentNo);
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole(role != null ? RoleEnum.getByCode(role) : RoleEnum.STUDENT);
        user.setStatus(UserStatusEnum.ACTIVE);
        user.setDepartment(department);
        user.setGrade(grade);
        user.setClassName(className);
        user.setWorkDepartment(workDepartment);
        user.setWorkDepartmentId(workDepartmentId);
        user.setPosition(position);

        return userRepository.save(user);
    }

    public boolean checkStudentNoExists(String studentNo) {
        if (studentNo == null || studentNo.trim().isEmpty()) {
            return false;
        }
        return userRepository.findByStudentNoAndDeletedFalse(studentNo).isPresent();
    }
}
