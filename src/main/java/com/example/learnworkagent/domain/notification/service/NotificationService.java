package com.example.learnworkagent.domain.notification.service;

import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.domain.notification.entity.Notification;
import com.example.learnworkagent.domain.notification.entity.NotificationMessage;

import java.util.List;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /**
     * 发送奖助申请审批结果通知
     *
     * @param message 通知消息
     */
    void sendAwardApprovalNotification(NotificationMessage message);

    /**
     * 获取用户的通知列表
     *
     * @param userId      用户ID
     * @param pageRequest 分页参数
     * @return 通知列表
     */
    PageResult<Notification> getUserNotifications(Long userId, PageRequest pageRequest);

    /**
     * 获取用户的未读通知
     *
     * @param userId 用户ID
     * @return 未读通知列表
     */
    List<Notification> getUnreadNotifications(Long userId);

    /**
     * 标记通知为已读
     *
     * @param notificationId 通知ID
     * @param userId         用户ID
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * 标记所有通知为已读
     *
     * @param userId 用户ID
     */
    void markAllAsRead(Long userId);

    /**
     * 获取未读通知数量
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    long getUnreadCount(Long userId);

    /**
     * 删除通知
     *
     * @param notificationId 通知ID
     * @param userId         用户ID
     */
    void deleteNotification(Long notificationId, Long userId);
}
