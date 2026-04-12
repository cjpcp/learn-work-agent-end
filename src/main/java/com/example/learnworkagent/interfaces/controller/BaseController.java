package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.domain.user.entity.Admin;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 基础控制器.
 * <p>提供通用的控制器功能，如获取当前登录用户ID等.</p>
 *
 * @author system
 */
@RequestMapping("/api/v1")
public abstract class BaseController {

    /**
     * 获取当前登录用户的ID.
     *
     * @return 当前用户ID，若未登录则返回null
     */
    protected Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Admin admin) {
            return admin.getId();
        }
        return null;
    }
}
