package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.domain.user.entity.Admin;
import com.example.learnworkagent.domain.user.entity.Role;
import com.example.learnworkagent.domain.user.entity.Teacher;
import com.example.learnworkagent.domain.user.repository.RoleRepository;
import com.example.learnworkagent.domain.user.repository.TeacherRepository;
import com.example.learnworkagent.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 系统控制器
 */
@Tag(name = "系统管理", description = "系统公共接口")
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController extends BaseController {

    private final RoleRepository roleRepository;
    private final TeacherRepository teacherRepository;
    private final UserService userService;

    @Operation(summary = "获取可选角色列表")
    @GetMapping("/roles/staff")
    public Result<List<Map<String, Object>>> getStaffRoles() {
        return Result.success(getRoleOptions());
    }

    @Operation(summary = "获取所有角色列表")
    @GetMapping("/roles")
    public Result<List<Map<String, Object>>> getAllRoles() {
        return Result.success(getRoleOptions());
    }

    @Operation(summary = "获取用户列表")
    @GetMapping("/users")
    public Result<List<Map<String, Object>>> getUsers(@RequestParam(required = false) Long roleId,
                                                      @RequestParam(required = false) String teacherKeyword) {
        List<Admin> admins = roleId != null ? userService.findByRoleId(roleId) : userService.findAll();
        String keyword = teacherKeyword == null ? "" : teacherKeyword.trim().toLowerCase();

        List<Map<String, Object>> result = admins.stream()
                .map(this::toUserOption)
                .filter(item -> keyword.isEmpty() || containsTeacherKeyword(item, keyword))
                .collect(Collectors.toList());
        return Result.success(result);
    }

    private List<Map<String, Object>> getRoleOptions() {
        return roleRepository.findAll().stream()
                .map(role -> Map.<String, Object>of(
                        "id", role.getId(),
                        "code", role.getRoleName(),
                        "name", role.getRoleName(),
                        "pagePath", role.getPagePath() == null ? "" : role.getPagePath()
                ))
                .collect(Collectors.toList());
    }

    private Map<String, Object> toUserOption(Admin admin) {
        Optional<Teacher> teacherOptional = teacherRepository.findById(admin.getTeacherId());
        Teacher teacher = teacherOptional.orElse(null);
        Role role = roleRepository.findById(admin.getRoleId()).orElse(null);
        String teacherName = teacher != null ? teacher.getName() : "";
        String cardNumber = teacher != null ? teacher.getCardNumber() : "";
        return Map.of(
                "id", admin.getId(),
                "username", admin.getUsername(),
                "nick", admin.getNick(),
                "teacherId", admin.getTeacherId(),
                "teacherName", teacherName,
                "cardNumber", cardNumber == null ? "" : cardNumber,
                "roleId", admin.getRoleId(),
                "roleName", role != null ? role.getRoleName() : "",
                "status", admin.getStatus(),
                "name", teacherName
        );
    }

    private boolean containsTeacherKeyword(Map<String, Object> item, String keyword) {
        String teacherName = String.valueOf(item.getOrDefault("teacherName", "")).toLowerCase();
        String cardNumber = String.valueOf(item.getOrDefault("cardNumber", "")).toLowerCase();
        String username = String.valueOf(item.getOrDefault("username", "")).toLowerCase();
        return teacherName.contains(keyword) || cardNumber.contains(keyword) || username.contains(keyword);
    }
}
