package com.example.learnworkagent.domain.notification.entity;

import com.example.learnworkagent.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 站内信通知实体.
 * <p>对应数据库中的notification表，存储系统发送给用户的通知消息.</p>
 *
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Comment("通知表")
@Table(name = "notification")
public class Notification extends BaseEntity {

    /** 接收用户ID。 */
    @Comment("接收用户ID")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 通知类型。 */
    @Comment("通知类型")
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    /** 通知标题。 */
    @Comment("通知标题")
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 通知内容。 */
    @Comment("通知内容")
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** 是否已读。 */
    @JsonProperty("isRead")
    @Comment("是否已读")
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /** 读取时间。 */
    @Comment("读取时间")
    @Column(name = "read_time")
    private LocalDateTime readTime;

    /** 关联业务ID。 */
    @Comment("关联业务ID")
    @Column(name = "business_id")
    private Long businessId;

    /** 关联业务类型。 */
    @Comment("关联业务类型")
    @Column(name = "business_type", length = 50)
    private String businessType;

    /** 推送渠道。 */
    @Comment("推送渠道")
    @Column(name = "channel", length = 20)
    private String channel;

    /** 各渠道发送状态。 */
    @Comment("各渠道发送状态")
    @Column(name = "channel_status", length = 500)
    private String channelStatus;

    public void markRead(LocalDateTime readTime) {
        this.isRead = Boolean.TRUE;
        this.readTime = readTime;
    }

    public static Notification fromMessage(NotificationMessage message, String channel) {
        return Notification.builder()
                .userId(message.getUserId())
                .type(message.getType())
                .title(message.getTitle())
                .content(message.getContent())
                .businessId(message.getBusinessId())
                .businessType(message.getBusinessType())
                .channel(channel)
                .channelStatus(channel + ":SUCCESS")
                .isRead(false)
                .build();
    }
}
