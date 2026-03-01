package com.example.learnworkagent.domain.notification.service.impl;

import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.domain.notification.entity.Notification;
import com.example.learnworkagent.domain.notification.entity.NotificationMessage;
import com.example.learnworkagent.domain.notification.repository.NotificationRepository;
import com.example.learnworkagent.domain.notification.service.NotificationService;
import com.example.learnworkagent.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void sendAwardApprovalNotification(NotificationMessage message) {
        log.info("发送奖助审批通知到消息队列，用户ID: {}, 申请ID: {}", message.getUserId(), message.getBusinessId());

        // 发送到RabbitMQ队列，由消费者异步处理多渠道推送
        rabbitTemplate.convertAndSend(RabbitMQConfig.APPROVAL_NOTIFICATION_QUEUE, message);
    }

    @Override
    public PageResult<Notification> getUserNotifications(Long userId, PageRequest pageRequest) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.getPage(),
                pageRequest.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        Page<Notification> page = notificationRepository.findByUserIdAndDeletedFalseOrderByCreateTimeDesc(userId, pageable);

        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                pageRequest.getPageNum(),
                pageRequest.getPageSize()
        );
    }

    @Override
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseAndDeletedFalseOrderByCreateTimeDesc(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此通知");
        }

        notification.setIsRead(true);
        notification.setReadTime(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository
                .findByUserIdAndIsReadFalseAndDeletedFalseOrderByCreateTimeDesc(userId);

        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notification.setReadTime(LocalDateTime.now());
        }

        notificationRepository.saveAll(unreadNotifications);
        log.info("用户 {} 标记所有通知为已读，共 {} 条", userId, unreadNotifications.size());
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalseAndDeletedFalse(userId);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除此通知");
        }

        notification.setDeleted(true);
        notificationRepository.save(notification);
    }
}
