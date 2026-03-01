package com.example.learnworkagent.infrastructure.external.notification;

import com.example.learnworkagent.domain.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket通知服务
 * 用于向客户端实时推送通知
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 向指定用户发送通知
     *
     * @param userId       用户ID
     * @param notification 通知内容
     */
    public void sendNotificationToUser(Long userId, Notification notification) {
        try {
            String destination = "/queue/notifications";
            messagingTemplate.convertAndSendToUser(userId.toString(), destination, notification);
            log.info("WebSocket通知已发送给用户: {}, 通知ID: {}", userId, notification.getId());
        } catch (Exception e) {
            log.error("WebSocket通知发送失败，用户ID: {}", userId, e);
        }
    }

    /**
     * 向指定用户发送未读数量更新
     *
     * @param userId      用户ID
     * @param unreadCount 未读数量
     */
    public void sendUnreadCountToUser(Long userId, long unreadCount) {
        try {
            String destination = "/queue/unread-count";
            messagingTemplate.convertAndSendToUser(userId.toString(), destination, new UnreadCountMessage(unreadCount));
            log.info("未读数量已推送给用户: {}, 数量: {}, 目标: {}", userId, unreadCount, destination);
        } catch (Exception e) {
            log.error("未读数量推送失败，用户ID: {}", userId, e);
        }
    }

    /**
     * 广播通知（用于系统公告等）
     *
     * @param notification 通知内容
     */
    public void broadcastNotification(Notification notification) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications", notification);
            log.info("广播通知已发送，通知ID: {}", notification.getId());
        } catch (Exception e) {
            log.error("广播通知发送失败", e);
        }
    }

    /**
     * 未读数量消息
     */
    public record UnreadCountMessage(long count) {
    }
}
