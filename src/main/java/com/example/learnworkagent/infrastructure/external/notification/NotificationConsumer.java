package com.example.learnworkagent.infrastructure.external.notification;

import com.example.learnworkagent.common.enums.NotificationChannelEnum;
import com.example.learnworkagent.domain.notification.entity.NotificationMessage;
import com.example.learnworkagent.domain.notification.service.NotificationSender;
import com.example.learnworkagent.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知消息消费者，负责多渠道推送。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private static final String CHANNEL_ALL = NotificationChannelEnum.ALL.getCode();
    private static final List<String> DEFAULT_CHANNELS = List.of(NotificationChannelEnum.SITE.getCode());

    private final List<NotificationSender> notificationSenders;

    /**
     * 处理审批通知消息。
     *
     * @param message 通知消息
     */
    @RabbitListener(queues = RabbitMQConfig.APPROVAL_NOTIFICATION_QUEUE)
    public void handleApprovalNotification(NotificationMessage message) {
        List<String> channels = resolveChannels(message);
        Map<String, NotificationSender> senderMap = buildSenderMap();

        log.info("接收到审批通知消息，用户ID: {}, 业务ID: {}, 渠道: {}",
                message.getUserId(), message.getBusinessId(), channels);

        for (String channel : channels) {
            sendByChannel(message, channel, senderMap);
        }
    }

    private Map<String, NotificationSender> buildSenderMap() {
        return notificationSenders.stream()
                .collect(Collectors.toMap(NotificationSender::getChannel, sender -> sender, (left, right) -> left));
    }

    private void sendByChannel(NotificationMessage message, String channel, Map<String, NotificationSender> senderMap) {
        NotificationSender sender = senderMap.get(channel);
        if (sender == null) {
            log.warn("未找到 {} 渠道的发送器", channel);
            return;
        }

        try {
            boolean success = sender.send(message);
            log.info("{} 渠道通知发送{}，用户ID: {}", channel, success ? "成功" : "失败", message.getUserId());
        } catch (Exception exception) {
            log.error("{} 渠道通知发送异常，用户ID: {}", channel, message.getUserId(), exception);
        }
    }

    private List<String> resolveChannels(NotificationMessage message) {
        if (message.getChannels() == null || message.getChannels().isEmpty()) {
            message.setChannels(DEFAULT_CHANNELS);
            return DEFAULT_CHANNELS;
        }

        Set<String> resolvedChannels = new LinkedHashSet<>();
        for (String channel : message.getChannels()) {
            if (CHANNEL_ALL.equals(channel)) {
                resolvedChannels.addAll(notificationSenders.stream()
                        .map(NotificationSender::getChannel)
                        .toList());
                continue;
            }
            resolvedChannels.add(channel);
        }

        List<String> channels = List.copyOf(resolvedChannels);
        message.setChannels(channels);
        return channels;
    }
}
