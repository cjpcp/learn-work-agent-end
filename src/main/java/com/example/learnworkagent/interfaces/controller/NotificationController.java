package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.domain.notification.entity.Notification;
import com.example.learnworkagent.domain.notification.service.NotificationService;
import com.example.learnworkagent.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "通知相关接口")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取当前用户的通知列表
     */
    @GetMapping
    @Operation(summary = "获取通知列表", description = "分页获取当前用户的通知列表")
    public Result<PageResult<Notification>> getNotifications(
            @AuthenticationPrincipal User user,
            PageRequest pageRequest) {

        PageResult<Notification> result = notificationService.getUserNotifications(user.getId(), pageRequest);
        return Result.success(result);
    }

    /**
     * 获取未读通知列表
     */
    @GetMapping("/unread")
    @Operation(summary = "获取未读通知", description = "获取当前用户的所有未读通知")
    public Result<List<Notification>> getUnreadNotifications(
            @AuthenticationPrincipal User user) {

        List<Notification> notifications = notificationService.getUnreadNotifications(user.getId());
        return Result.success(notifications);
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread/count")
    @Operation(summary = "获取未读数量", description = "获取当前用户的未读通知数量")
    public Result<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal User user) {

        long count = notificationService.getUnreadCount(user.getId());
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return Result.success(result);
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "标记已读", description = "将指定通知标记为已读")
    public Result<Void> markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        notificationService.markAsRead(id, user.getId());
        return Result.success();
    }

    /**
     * 标记所有通知为已读
     */
    @PutMapping("/read-all")
    @Operation(summary = "全部已读", description = "将所有通知标记为已读")
    public Result<Void> markAllAsRead(
            @AuthenticationPrincipal User user) {

        notificationService.markAllAsRead(user.getId());
        return Result.success();
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知", description = "删除指定通知")
    public Result<Void> deleteNotification(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        notificationService.deleteNotification(id, user.getId());
        return Result.success();
    }
}
