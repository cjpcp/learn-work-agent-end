package com.example.learnworkagent.domain.user.service;

import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.domain.user.entity.Admin;


/**
 * 用户服务接口.
 * <p>定义用户管理的业务逻辑.</p>
 *
 * @author system
 */
public interface UserService {

    /**
     * 组合条件分页查询用户账户.
     *
     * @param roleId    角色ID筛选（可选）
     * @param username  用户名筛选（可选）
     * @param nick      昵称筛选（可选）
     * @param status    状态筛选（可选）
     * @param pageRequest 分页参数
     * @return 分页后的用户列表
     */
    PageResult<Admin> findUsersPage(Long roleId, String username, String nick, Integer status, PageRequest pageRequest);
}
