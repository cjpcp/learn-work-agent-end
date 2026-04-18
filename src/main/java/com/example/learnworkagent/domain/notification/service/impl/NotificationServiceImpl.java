package com.example.learnworkagent.domain.notification.service.impl;

import com.example.learnworkagent.common.ResultCode;
import com.example.learnworkagent.common.dto.PageRequest;
import com.example.learnworkagent.common.dto.PageResult;
import com.example.learnworkagent.common.exception.BusinessException;
import com.example.learnworkagent.domain.notification.entity.Notification;
import com.example.learnworkagent.domain.notification.entity.NotificationMessage;
import com.example.learnworkagent.domain.notification.repository.NotificationRepository;
import com.example.learnworkagent.domain.notification.service.NotificationService;
import com.example.learnworkagent.infrastructure.config.RabbitMQConfig;
import com.example.learnworkagent.infrastructure.external.notification.WebSocketNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 通知服务实现。
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final String SORT_FIELD_CREATE_TIME = "createTime";
    private static final String FORBIDDEN_READ_MESSAGE = "无权操作此通知";

    private final NotificationRepository notificationRepository;
    private final RabbitTemplate rabbitTemplate;
    private final WebSocketNotificationService webSocketNotificationService;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   RabbitTemplate rabbitTemplate,
                                   @Lazy WebSocketNotificationService webSocketNotificationService) {
        this.notificationRepository = notificationRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.webSocketNotificationService = webSocketNotificationService;
    }

    @Override
    public void sendAwardApprovalNotification(NotificationMessage message) {
        log.info("发送审批通知到消息队列，用户ID: {}, 业务ID: {}", message.getUserId(), message.getBusinessId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.APPROVAL_NOTIFICATION_QUEUE, message);
    }

    @Override
    public PageResult<Notification> getUserNotifications(Long userId, PageRequest pageRequest) {
        Pageable pageable = buildPageable(pageRequest);
        Page<Notification> page = notificationRepository.findByUserIdAndDeletedFalseOrderByCreateTimeDesc(userId, pageable);
        return buildPageResult(page, pageRequest);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = getOwnedNotification(notificationId, userId, FORBIDDEN_READ_MESSAGE);
        notification.markRead(LocalDateTime.now());
        notificationRepository.save(notification);
        pushUnreadCount(userId);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalseAndDeletedFalse(userId);
    }

    private Notification getOwnedNotification(Long notificationId, Long userId, String forbiddenMessage) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, forbiddenMessage);
        }
        return notification;
    }

    private Pageable buildPageable(PageRequest pageRequest) {
        return org.springframework.data.domain.PageRequest.of(
                pageRequest.getPage(),
                pageRequest.getPageSize(),
                Sort.by(Sort.Direction.DESC, SORT_FIELD_CREATE_TIME)
        );
    }

    private PageResult<Notification> buildPageResult(Page<Notification> page, PageRequest pageRequest) {
        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                pageRequest.getPageNum(),
                pageRequest.getPageSize()
        );
    }

    private void pushUnreadCount(Long userId) {
        long unreadCount = getUnreadCount(userId);
        webSocketNotificationService.sendUnreadCountToUser(userId, unreadCount);
    }
}
