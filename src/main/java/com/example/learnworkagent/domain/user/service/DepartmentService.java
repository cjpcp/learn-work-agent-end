package com.example.learnworkagent.domain.user.service;

import com.example.learnworkagent.common.enums.DepartmentTypeEnum;
import com.example.learnworkagent.common.enums.UserStatusEnum;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.domain.user.entity.Department;
import com.example.learnworkagent.domain.user.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 部门服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    /**
     * 获取所有启用的部门列表
     */
    public List<Department> getEnabledDepartments() {
        return departmentRepository.findByEnabledAndDeletedFalseOrderBySortOrderAsc(true);
    }

    /**
     * 获取所有启用的学院列表
     */
    public List<Department> getEnabledColleges() {
        return departmentRepository.findByTypeAndEnabledAndDeletedFalseOrderBySortOrderAsc(DepartmentTypeEnum.COLLEGE, true);
    }

    /**
     * 获取所有启用的部门列表
     */
    public List<Department> getEnabledDepartmentsByType() {
        return departmentRepository.findByTypeAndEnabledAndDeletedFalseOrderBySortOrderAsc(DepartmentTypeEnum.DEPARTMENT, true);
    }

    /**
     * 获取所有部门列表
     */
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    /**
     * 根据ID获取部门
     */
    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.FAIL, "部门不存在"));
    }

    /**
     * 根据代码获取部门
     */
    public Department getDepartmentByCode(String code) {
        return departmentRepository.findByCodeAndDeletedFalse(code)
                .orElseThrow(() -> new BusinessException(ResultCode.FAIL, "部门不存在"));
    }

    /**
     * 创建部门
     */
    @Transactional
    public Department createDepartment(String code, String name, String description, Integer sortOrder, DepartmentTypeEnum type) {
        if (departmentRepository.existsByCodeAndDeletedFalse(code)) {
            throw new BusinessException(ResultCode.FAIL, "部门编码已存在");
        }

        Department department = new Department();
        department.setCode(code);
        department.setName(name);
        department.setDescription(description);
        department.setSortOrder(sortOrder != null ? sortOrder : 0);
        department.setEnabled(true);
        department.setType(type);

        return departmentRepository.save(department);
    }

    /**
     * 更新部门
     */
    @Transactional
    public Department updateDepartment(Long id, String name, String description, Integer sortOrder, Boolean enabled) {
        Department department = getDepartmentById(id);
        
        if (name != null) {
            department.setName(name);
        }
        if (description != null) {
            department.setDescription(description);
        }
        if (sortOrder != null) {
            department.setSortOrder(sortOrder);
        }
        if (enabled != null) {
            department.setEnabled(enabled);
        }

        return departmentRepository.save(department);
    }

    /**
     * 删除部门
     */
    @Transactional
    public void deleteDepartment(Long id) {
        Department department = getDepartmentById(id);
        department.setDeleted(true);
        departmentRepository.save(department);
    }
}
