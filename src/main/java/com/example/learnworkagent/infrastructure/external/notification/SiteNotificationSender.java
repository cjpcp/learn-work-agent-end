package com.example.learnworkagent.infrastructure.external.notification;

import com.example.learnworkagent.domain.notification.entity.Notification;
import com.example.learnworkagent.domain.notification.entity.NotificationMessage;
import com.example.learnworkagent.domain.notification.repository.NotificationRepository;
import com.example.learnworkagent.domain.notification.service.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 站内信通知发送器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SiteNotificationSender implements NotificationSender {

    private final NotificationRepository notificationRepository;

    @Override
    public String getChannel() {
        return "SITE";
    }

    @Override
    public boolean send(NotificationMessage message) {
        try {
            log.info("发送站内信通知，用户ID: {}, 标题: {}", message.getUserId(), message.getTitle());

            Notification notification = new Notification();
            notification.setUserId(message.getUserId());
            notification.setType(message.getType());
            notification.setTitle(message.getTitle());
            notification.setContent(message.getContent());
            notification.setBusinessId(message.getBusinessId());
            notification.setBusinessType(message.getBusinessType());
            notification.setChannel(getChannel());

            notificationRepository.save(notification);

            log.info("站内信通知发送成功，通知ID: {}", notification.getId());
            return true;
        } catch (Exception e) {
            log.error("站内信通知发送失败，用户ID: {}", message.getUserId(), e);
            return false;
        }
    }
}
