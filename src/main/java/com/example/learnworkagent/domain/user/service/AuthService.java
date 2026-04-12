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
 * 认证服务.
 * <p>提供用户登录、注册、密码校验等认证相关业务逻辑.</p>
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long NO_TEACHER_ID = 0L;

    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RsaUtil rsaUtil;

    /**
     * 用户登录.
     * <p>验证用户名密码，校验账户状态，构建登录响应（含JWT Token）.</p>
     *
     * @param request 登录请求（用户名、密码）
     * @return 登录成功响应（Token、用户信息）
     * @throws BusinessException 用户名不存在、密码错误、账户被禁用时抛出
     */
    public LoginResponse login(LoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误"));

        if (!admin.isEnabled()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账户已被禁用");
        }

        Role role = roleRepository.findById(admin.getRoleId())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "账户角色不存在"));

        String decryptedPassword = rsaUtil.decrypt(request.getPassword());
        if (!passwordEncoder.matches(decryptedPassword, admin.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        Teacher teacher = loadTeacherIfPresent(admin.getTeacherId());
        if (teacher != null && !teacher.isEnabled()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "教师已被禁用");
        }

        admin.setLoginTime(LocalDateTime.now());
        adminRepository.save(admin);

        return buildLoginResponse(admin, role, teacher);
    }

    /**
     * 用户注册.
     * <p>创建新用户账号，若需要同时创建关联的教师信息.</p>
     *
     * @param request 注册请求（用户名、密码、角色等）
     * @return 注册成功响应（Token、用户信息）
     * @throws BusinessException 用户名已存在、角色不存在时抛出
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (adminRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "用户名已存在");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "角色不存在"));

        String decryptedPassword = rsaUtil.decrypt(request.getPassword());
        Teacher teacher = createTeacherIfRequired(request);

        Admin admin = new Admin();
        admin.setId(null);
        admin.setUsername(request.getUsername());
        admin.setNick(request.getNick());
        admin.setPassword(passwordEncoder.encode(decryptedPassword));
        admin.setRoleId(role.getId());
        admin.setStatus(1);
        admin.setTeacherId(teacher != null ? teacher.getId() : NO_TEACHER_ID);
        Admin savedAdmin = adminRepository.save(admin);

        return buildLoginResponse(savedAdmin, role, teacher);
    }

    /**
     * 检查用户名是否已被使用.
     *
     * @param username 待检查的用户名
     * @return true表示用户名已存在，false表示可用
     */
    public boolean checkUsernameExists(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return adminRepository.existsByUsername(username.trim());
    }

    /**
     * 根据注册请求创建教师信息（仅当teacher字段为true时创建）.
     *
     * @param request 注册请求
     * @return 创建的教师实体，若不需要创建则返回null
     */
    private Teacher createTeacherIfRequired(RegisterRequest request) {
        if (!Boolean.TRUE.equals(request.getTeacher())) {
            return null;
        }

        validateTeacherFields(request);
        Teacher teacher = new Teacher();
        teacher.setId(null);
        teacher.setName(request.getTeacherName().trim());
        teacher.setPhone(request.getPhone().trim());
        teacher.setCardNumber(request.getCardNumber().trim());
        teacher.setState(1);
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        teacher.setCreateTime(timestamp);
        teacher.setUpdateTime(timestamp);
        return teacherRepository.save(teacher);
    }

    /**
     * 根据教师ID加载教师信息（若存在）.
     *
     * @param teacherId 教师ID
     * @return 教师实体，若不存在则返回null
     */
    private Teacher loadTeacherIfPresent(Long teacherId) {
        if (teacherId == null || teacherId <= 0) {
            return null;
        }
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "账户关联教师不存在"));
    }

    /**
     * 构建登录响应对象.
     *
     * @param admin   管理员实体
     * @param role    角色实体
     * @param teacher 教师实体（可为null）
     * @return 登录响应对象
     */
    private LoginResponse buildLoginResponse(Admin admin, Role role, Teacher teacher) {
        String token = jwtUtil.generateToken(admin.getId(), admin.getUsername(), role.getRoleName());
        return new LoginResponse(
                token,
                admin.getId(),
                admin.getUsername(),
                admin.getNick(),
                teacher != null ? teacher.getId() : NO_TEACHER_ID,
                teacher != null ? teacher.getName() : "",
                role.getId(),
                role.getRoleName(),
                admin.getStatus()
        );
    }

    /**
     * 校验教师相关字段是否完整.
     *
     * @param request 注册请求
     * @throws BusinessException 字段为空时抛出
     */
    private void validateTeacherFields(RegisterRequest request) {
        if (isBlank(request.getTeacherName())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "教师姓名不能为空");
        }
        if (isBlank(request.getPhone())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "联系电话不能为空");
        }
        if (isBlank(request.getCardNumber())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "学工号不能为空");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
