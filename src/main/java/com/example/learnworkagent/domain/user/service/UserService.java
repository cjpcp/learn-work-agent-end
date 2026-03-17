package com.example.learnworkagent.domain.user.service;

import com.example.learnworkagent.common.enums.RoleEnum;
import com.example.learnworkagent.domain.user.entity.User;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 获取所有用户
     */
    List<User> findAll();

    /**
     * 根据工作部门ID和角色查询
     */
    List<User> findByDepartmentIdAndRole(Long workDepartmentId, String role);
}
