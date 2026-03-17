package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;

import com.example.learnworkagent.common.enums.RoleEnum;
import com.example.learnworkagent.domain.user.entity.Department;
import com.example.learnworkagent.domain.user.entity.User;
import com.example.learnworkagent.domain.user.service.DepartmentService;
import com.example.learnworkagent.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统控制器
 * 提供系统级别的公共接口
 */
@Tag(name = "系统管理", description = "系统公共接口")
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController extends BaseController {

    private final DepartmentService departmentService;
    private final UserService userService;

    /**
     * 获取学工角色列表
     */
    @Operation(summary = "获取学工角色列表")
    @GetMapping("/roles/staff")
    public Result<List<Map<String, String>>> getStaffRoles() {
        List<Map<String, String>> roles = Arrays.stream(RoleEnum.getStaffRoles())
                .map(role -> Map.of(
                        "code", role.getCode(),
                        "name", role.getName()
                ))
                .collect(Collectors.toList());
        return Result.success(roles);
    }

    /**
     * 获取所有角色列表
     */
    @Operation(summary = "获取所有角色列表")
    @GetMapping("/roles")
    public Result<List<Map<String, String>>> getAllRoles() {
        List<Map<String, String>> roles = Arrays.stream(RoleEnum.values())
                .map(role -> Map.of(
                        "code", role.getCode(),
                        "name", role.getName()
                ))
                .collect(Collectors.toList());
        return Result.success(roles);
    }

    /**
     * 获取启用的部门列表
     */
    @Operation(summary = "获取启用的部门列表")
    @GetMapping("/departments")
    public Result<List<Map<String, Object>>> getDepartments() {
        List<Department> departments = departmentService.getEnabledDepartmentsByType();
        return getListResult(departments);
    }

    /**
     * 获取启用的学院列表
     */
    @Operation(summary = "获取启用的学院列表")
    @GetMapping("/colleges")
    public Result<List<Map<String, Object>>> getColleges() {
        List<Department> colleges = departmentService.getEnabledColleges();
        return getListResult(colleges);
    }

    private Result<List<Map<String, Object>>> getListResult(List<Department> colleges) {
        List<Map<String, Object>> result = colleges.stream()
                .map(college -> Map.<String, Object>of(
                        "id", college.getId(),
                        "code", college.getCode(),
                        "name", college.getName(),
                        "description", college.getDescription()
                ))
                .collect(Collectors.toList());
        return Result.success(result);
    }

    /**
     * 获取用户列表
     */
    @Operation(summary = "获取用户列表")
    @GetMapping("/users")
    public Result<List<Map<String, Object>>> getUsers(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long departmentId) {
        List<User> users;
        if (departmentId != null) {
            // 使用部门ID查询用户
            users = userService.findByDepartmentIdAndRole(departmentId, RoleEnum.DEPARTMENT_LEADER.getCode());
        } else {
            users = userService.findAll();
        }
        List<Map<String, Object>> result = users.stream()
                .map(user -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", user.getId());
                    map.put("name", user.getRealName());
                    map.put("username", user.getUsername());
                    map.put("role", user.getRole());
                    return map;
                })
                .collect(Collectors.toList());
        return Result.success(result);
    }
}
