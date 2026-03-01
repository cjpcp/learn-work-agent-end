package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.domain.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 基础控制器
 */
@RequestMapping("/api/v1")
public abstract class BaseController {

    /**
     * 获取当前用户ID（从SecurityContext中获取）
     * SecurityContext中包含principal（用户个体信息），credentials（凭证如密码），authorities（用户权限集合）等
     */
    protected Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getId();
        }
        return null;
    }

    /**
     * 获取当前用户名（从SecurityContext中获取）
     * todo将当前用户名存储到SecurityContext的authentication的name中
     */
    protected String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        return null;
    }
}
