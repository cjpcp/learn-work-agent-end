package com.example.learnworkagent.infrastructure.external.notification;

import com.example.learnworkagent.domain.notification.entity.NotificationMessage;
import com.example.learnworkagent.domain.notification.service.NotificationSender;
import com.example.learnworkagent.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知消息消费者 - 处理多渠道推送
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final List<NotificationSender> notificationSenders;

    /**
     * 处理审批通知消息
     */
    @RabbitListener(queues = RabbitMQConfig.APPROVAL_NOTIFICATION_QUEUE)
    public void handleApprovalNotification(NotificationMessage message) {
        log.info("接收到审批通知消息，用户ID: {}, 申请ID: {}, 渠道: {}",
                message.getUserId(), message.getBusinessId(), message.getChannels());

        if (message.getChannels() == null || message.getChannels().isEmpty()) {
            // 默认使用站内信
            message.setChannels(List.of("SITE"));
        }

        // 构建发送器映射
        Map<String, NotificationSender> senderMap = notificationSenders.stream()
                .collect(Collectors.toMap(NotificationSender::getChannel, sender -> sender));

        // 遍历所有渠道发送通知
        for (String channel : message.getChannels()) {
            NotificationSender sender = senderMap.get(channel);
            if (sender != null) {
                try {
                    boolean success = sender.send(message);
                    log.info("{} 渠道通知发送{}，用户ID: {}",
                            channel, success ? "成功" : "失败", message.getUserId());
                } catch (Exception e) {
                    log.error("{} 渠道通知发送异常，用户ID: {}", channel, message.getUserId(), e);
                }
            } else {
                log.warn("未找到 {} 渠道的发送器", channel);
            }
        }
    }
}
