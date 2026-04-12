package com.example.learnworkagent.domain.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 通知消息实体.
 * <p>用于RabbitMQ消息队列传输的通知信息，包含接收人、消息内容等.</p>
 *
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 接收用户ID。 */
    private Long userId;

    /** 用户手机号。 */
    private String phone;

    /** 用户邮箱。 */
    private String email;

    /** 通知类型。 */
    private String type;

    /** 通知标题。 */
    private String title;

    /** 通知内容。 */
    private String content;

    /** 关联业务ID。 */
    private Long businessId;

    /** 关联业务类型。 */
    private String businessType;

    /** 推送渠道列表。 */
    private List<String> channels;

    /** 接收人姓名。 */
    private String receiverName;

    /** 申请人姓名。 */
    private String applicantName;

    /** 申请类型。 */
    private String applicationType;

    /** 奖项名称或请假类型名称。 */
    private String awardName;

    /** 申请金额。 */
    private BigDecimal amount;

    /** 审批状态。 */
    private String approvalStatus;

    /** 审批意见。 */
    private String approvalComment;

    /** 审批人姓名。 */
    private String approverName;

    /** 请假开始日期。 */
    private LocalDate leaveStartDate;

    /** 请假结束日期。 */
    private LocalDate leaveEndDate;

    /** 请假天数。 */
    private Integer leaveDays;

    /** 请假原因。 */
    private String leaveReason;
}
