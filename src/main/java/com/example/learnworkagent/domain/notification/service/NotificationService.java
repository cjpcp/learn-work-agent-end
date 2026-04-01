package com.example.learnworkagent.domain.notification.service;

import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.domain.notification.entity.Notification;
import com.example.learnworkagent.domain.notification.entity.NotificationMessage;

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
     * 标记通知为已读
     *
     * @param notificationId 通知ID
     * @param userId         用户ID
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * 获取未读通知数量
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    long getUnreadCount(Long userId);

}
