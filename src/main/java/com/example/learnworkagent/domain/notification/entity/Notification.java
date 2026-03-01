package com.example.learnworkagent.domain.notification.entity;

import com.example.learnworkagent.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 通知实体 - 站内信
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "notification")
public class Notification extends BaseEntity {

    /**
     * 接收用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 通知类型（AWARD_APPROVAL-奖助审批结果, SYSTEM-系统通知）
     */
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    /**
     * 通知标题
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * 通知内容
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * 是否已读
     */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /**
     * 读取时间
     */
    @Column(name = "read_time")
    private LocalDateTime readTime;

    /**
     * 关联业务ID（如奖助申请ID）
     */
    @Column(name = "business_id")
    private Long businessId;

    /**
     * 关联业务类型
     */
    @Column(name = "business_type", length = 50)
    private String businessType;

    /**
     * 推送渠道（SITE-站内信, EMAIL-邮件, SMS-短信, ALL-全部）
     */
    @Column(name = "channel", length = 20)
    private String channel;

    /**
     * 各渠道发送状态（JSON格式）
     */
    @Column(name = "channel_status", length = 500)
    private String channelStatus;
}
