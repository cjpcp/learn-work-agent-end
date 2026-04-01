package com.example.learnworkagent.infrastructure.external.notification;

import com.example.learnworkagent.common.enums.NotificationChannelEnum;
import com.example.learnworkagent.domain.notification.entity.Notification;
import com.example.learnworkagent.domain.notification.entity.NotificationMessage;
import com.example.learnworkagent.domain.notification.repository.NotificationRepository;
import com.example.learnworkagent.domain.notification.service.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 站内信通知发送器。
 */
@Slf4j
@Component
public class SiteNotificationSender implements NotificationSender {

    private final NotificationRepository notificationRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    public SiteNotificationSender(NotificationRepository notificationRepository,
                                  @Lazy WebSocketNotificationService webSocketNotificationService) {
        this.notificationRepository = notificationRepository;
        this.webSocketNotificationService = webSocketNotificationService;
    }

    @Override
    public String getChannel() {
        return NotificationChannelEnum.SITE.getCode();
    }

    @Override
    public boolean send(NotificationMessage message) {
        try {
            log.info("发送站内信通知，用户ID: {}, 标题: {}", message.getUserId(), message.getTitle());

            Notification notification = Notification.fromMessage(message, getChannel());
            notificationRepository.save(notification);

            webSocketNotificationService.sendNotificationToUser(message.getUserId(), notification);
            pushUnreadCount(message.getUserId());

            log.info("站内信通知发送成功，通知ID: {}", notification.getId());
            return true;
        } catch (Exception exception) {
            log.error("站内信通知发送失败，用户ID: {}", message.getUserId(), exception);
            return false;
        }
    }

    private void pushUnreadCount(Long userId) {
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalseAndDeletedFalse(userId);
        webSocketNotificationService.sendUnreadCountToUser(userId, unreadCount);
    }
}
