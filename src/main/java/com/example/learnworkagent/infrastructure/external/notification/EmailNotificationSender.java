package com.example.learnworkagent.infrastructure.external.notification;

import com.example.learnworkagent.domain.notification.entity.NotificationMessage;
import com.example.learnworkagent.domain.notification.service.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * 邮件通知发送器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${mail.mock.enabled:false}")
    private boolean mockEnabled;

    @Override
    public String getChannel() {
        return "EMAIL";
    }

    @Override
    public boolean send(NotificationMessage message) {
        if (message.getEmail() == null || message.getEmail().isEmpty()) {
            log.warn("用户邮箱为空，无法发送邮件通知，用户ID: {}", message.getUserId());
            return false;
        }

        try {
            log.info("发送邮件通知到: {}, 标题: {}", maskEmail(message.getEmail()), message.getTitle());

            if (mockEnabled) {
                log.info("【邮件模拟发送】邮箱: {}, 内容: {}",
                        maskEmail(message.getEmail()), buildEmailContent(message));
                return true;
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(message.getEmail());
            helper.setSubject(message.getTitle());
            helper.setText(buildEmailContent(message), true);

            mailSender.send(mimeMessage);

            log.info("邮件通知发送成功: {}", maskEmail(message.getEmail()));
            return true;
        } catch (MessagingException e) {
            log.error("邮件通知发送失败: {}", maskEmail(message.getEmail()), e);
            return false;
        }
    }

    /**
     * 构建邮件内容（HTML格式）
     */
    private String buildEmailContent(NotificationMessage message) {
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html>\n");
        content.append("<html>\n<head>\n");
        content.append("<meta charset=\"UTF-8\">\n");
        content.append("<style>\n");
        content.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }\n");
        content.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }\n");
        content.append(".header { background-color: #1890ff; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }\n");
        content.append(".content { background-color: #f5f5f5; padding: 20px; border-radius: 0 0 5px 5px; }\n");
        content.append(".info-item { margin: 10px 0; padding: 10px; background-color: white; border-left: 4px solid #1890ff; }\n");
        content.append(".status-approved { color: #52c41a; font-weight: bold; }\n");
        content.append(".status-rejected { color: #f5222d; font-weight: bold; }\n");
        content.append(".footer { margin-top: 20px; text-align: center; color: #999; font-size: 12px; }\n");
        content.append("</style>\n</head>\n<body>\n");
        content.append("<div class=\"container\">\n");
        content.append("<div class=\"header\">\n");
        content.append("<h2>").append(message.getTitle()).append("</h2>\n");
        content.append("</div>\n");
        content.append("<div class=\"content\">\n");
        content.append("<p>尊敬的 ").append(message.getApplicantName()).append("，您好！</p>\n");
        content.append("<p>您的奖助申请已有审批结果，详情如下：</p>\n");
        content.append("<div class=\"info-item\">\n");
        content.append("<strong>申请类型：</strong>").append(getApplicationTypeName(message.getApplicationType())).append("<br>\n");
        content.append("<strong>奖项名称：</strong>").append(message.getAwardName()).append("<br>\n");
        content.append("<strong>申请金额：</strong>¥").append(message.getAmount()).append("<br>\n");
        content.append("<strong>审批状态：</strong>");

        if ("APPROVED".equals(message.getApprovalStatus())) {
            content.append("<span class=\"status-approved\">已通过</span>");
        } else {
            content.append("<span class=\"status-rejected\">未通过</span>");
        }
        content.append("<br>\n");

        content.append("<strong>审批意见：</strong>").append(message.getApprovalComment()).append("<br>\n");
        content.append("<strong>审批人：</strong>").append(message.getApproverName()).append("\n");
        content.append("</div>\n");
        content.append("<p>如有疑问，请联系学工处咨询。</p>\n");
        content.append("</div>\n");
        content.append("<div class=\"footer\">\n");
        content.append("<p>此邮件由系统自动发送，请勿回复</p>\n");
        content.append("<p>校园智能服务平台</p>\n");
        content.append("</div>\n");
        content.append("</div>\n");
        content.append("</body>\n</html>");

        return content.toString();
    }

    private String getApplicationTypeName(String type) {
        return switch (type) {
            case "SCHOLARSHIP" -> "奖学金";
            case "GRANT" -> "助学金";
            case "SUBSIDY" -> "困难补助";
            default -> type;
        };
    }

    /**
     * 邮箱脱敏
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        if (localPart.length() <= 2) {
            return email;
        }
        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + "@" + parts[1];
    }
}
