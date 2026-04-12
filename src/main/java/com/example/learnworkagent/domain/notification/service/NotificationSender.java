package com.example.learnworkagent.domain.notification.service;

import com.example.learnworkagent.domain.notification.entity.NotificationMessage;

/**
 * 通知发送器接口.
 * <p>定义通知发送的策略接口，支持多种发送渠道（如邮件、短信等）.</p>
 *
 * @author system
 */
public interface NotificationSender {

    /**
     * 获取发送渠道类型.
     *
     * @return 渠道类型标识
     */
    String getChannel();

    /**
     * 发送通知消息.
     *
     * @param message 通知消息内容
     * @return true表示发送成功，false表示发送失败
     */
    boolean send(NotificationMessage message);
}
