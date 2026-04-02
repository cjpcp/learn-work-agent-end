package com.example.learnworkagent.domain.user.service;

import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.dto.LoginRequest;
import com.example.learnworkagent.common.dto.LoginResponse;
import com.example.learnworkagent.common.dto.RegisterRequest;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.util.JwtUtil;
import com.example.learnworkagent.common.util.RsaUtil;
import com.example.learnworkagent.domain.user.entity.Admin;
import com.example.learnworkagent.domain.user.entity.Role;
import com.example.learnworkagent.domain.user.entity.Teacher;
import com.example.learnworkagent.domain.user.repository.AdminRepository;
import com.example.learnworkagent.domain.user.repository.RoleRepository;
import com.example.learnworkagent.domain.user.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RsaUtil rsaUtil;

    public LoginResponse login(LoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误"));

        if (!admin.isEnabled()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账户已被禁用");
        }

        Teacher teacher = teacherRepository.findById(admin.getTeacherId())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "账户关联教师不存在"));
        if (!teacher.isEnabled()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "教师已被禁用");
        }

        Role role = roleRepository.findById(admin.getRoleId())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "账户角色不存在"));

        String decryptedPassword = rsaUtil.decrypt(request.getPassword());
        if (!passwordEncoder.matches(decryptedPassword, admin.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        admin.setLoginTime(LocalDateTime.now());
        adminRepository.save(admin);

        String token = jwtUtil.generateToken(admin.getId(), admin.getUsername(), role.getRoleName());
        return new LoginResponse(
                token,
                admin.getId(),
                admin.getUsername(),
                admin.getNick(),
                teacher.getId(),
                teacher.getName(),
                role.getId(),
                role.getRoleName(),
                admin.getStatus()
        );
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (adminRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "用户名已存在");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "角色不存在"));

        String decryptedPassword = rsaUtil.decrypt(request.getPassword());

        Teacher teacher = new Teacher();
        teacher.setId(null);
        teacher.setName(request.getTeacherName());
        teacher.setPhone(request.getPhone());
        teacher.setCardNumber(request.getCardNumber());
        teacher.setState(1);
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        teacher.setCreateTime(timestamp);
        teacher.setUpdateTime(timestamp);
        Teacher savedTeacher = teacherRepository.save(teacher);

        Admin admin = new Admin();
        admin.setId(null);
        admin.setUsername(request.getUsername());
        admin.setNick(request.getNick());
        admin.setPassword(passwordEncoder.encode(decryptedPassword));
        admin.setRoleId(role.getId());
        admin.setStatus(1);
        admin.setTeacherId(savedTeacher.getId());
        Admin savedAdmin = adminRepository.save(admin);

        String token = jwtUtil.generateToken(savedAdmin.getId(), savedAdmin.getUsername(), role.getRoleName());
        return new LoginResponse(
                token,
                savedAdmin.getId(),
                savedAdmin.getUsername(),
                savedAdmin.getNick(),
                savedTeacher.getId(),
                savedTeacher.getName(),
                role.getId(),
                role.getRoleName(),
                savedAdmin.getStatus()
        );
    }

    public boolean checkUsernameExists(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return adminRepository.existsByUsername(username.trim());
    }
}
