package com.example.learnworkagent.domain.user.service;

import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.domain.user.entity.Admin;


/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 组合条件分页查询账户
     */
    PageResult<Admin> findUsersPage(Long roleId, String username, String nick, Integer status, PageRequest pageRequest);
}
