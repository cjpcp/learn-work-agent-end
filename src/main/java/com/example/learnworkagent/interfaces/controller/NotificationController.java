package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.domain.notification.entity.Notification;
import com.example.learnworkagent.domain.notification.service.NotificationService;
import com.example.learnworkagent.domain.user.entity.Admin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知控制器.
 * <p>提供通知的查询和状态管理接口.</p>
 *
 * @author system
 * @see NotificationService
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "通知相关接口")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 分页获取当前用户的通知列表.
     *
     * @param admin       当前登录用户
     * @param pageRequest 分页参数
     * @return 分页后的通知列表
     */
    @Operation(summary = "获取通知列表", description = "分页获取当前用户的通知列表")
    @GetMapping
    public Result<PageResult<Notification>> getNotifications(@AuthenticationPrincipal Admin admin, PageRequest pageRequest) {
        return Result.success(notificationService.getUserNotifications(admin.getId(), pageRequest));
    }

    /**
     * 获取当前用户的未读通知数量.
     *
     * @param admin 当前登录用户
     * @return 未读通知数量
     */
    @Operation(summary = "获取未读数量", description = "获取当前用户的未读通知数量")
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal Admin admin) {
        long count = notificationService.getUnreadCount(admin.getId());
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return Result.success(result);
    }

    /**
     * 将指定通知标记为已读.
     *
     * @param admin 当前登录用户
     * @param id    通知ID
     * @return 操作结果
     */
    @Operation(summary = "标记已读", description = "将指定通知标记为已读")
    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(@AuthenticationPrincipal Admin admin, @PathVariable Long id) {
        notificationService.markAsRead(id, admin.getId());
        return Result.success();
    }
}
