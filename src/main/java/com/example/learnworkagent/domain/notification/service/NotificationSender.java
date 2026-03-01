package com.example.learnworkagent.domain.notification.service;

import com.example.learnworkagent.domain.notification.entity.NotificationMessage;

/**
 * 通知发送器接口 - 策略模式
 */
public interface NotificationSender {

    /**
     * 获取发送渠道类型
     *
     * @return 渠道类型
     */
    String getChannel();

    /**
     * 发送通知
     *
     * @param message 通知消息
     * @return 是否发送成功
     */
    boolean send(NotificationMessage message);
}
