package com.example.learnworkagent.domain.user.service;

import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
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

    /**
     * 组合条件查询账户
     */
    List<Admin> findUsers(Long roleId, String username, String nick, Integer status);

    /**
     * 组合条件分页查询账户
     */
    PageResult<Admin> findUsersPage(Long roleId, String username, String nick, Integer status, PageRequest pageRequest);
}
