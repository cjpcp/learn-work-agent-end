package com.example.learnworkagent.domain.user.service;

import com.example.learnworkagent.domain.user.entity.Admin;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 获取所有账户
     */
    List<Admin> findAll();

    /**
     * 根据角色查询账户
     */
    List<Admin> findByRoleId(Long roleId);
}
