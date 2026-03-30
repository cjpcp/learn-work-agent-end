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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 邮件通知发送器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

            String html = buildEmailContent(message);
            if (mockEnabled) {
                log.info("【邮件模拟发送】邮箱: {}, 内容: {}", maskEmail(message.getEmail()), html);
                return true;
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(message.getEmail());
            helper.setSubject(message.getTitle());
            helper.setText(html, true);
            mailSender.send(mimeMessage);

            log.info("邮件通知发送成功: {}", maskEmail(message.getEmail()));
            return true;
        } catch (MessagingException e) {
            log.error("邮件通知发送失败: {}", maskEmail(message.getEmail()), e);
            return false;
        }
    }

    private String buildEmailContent(NotificationMessage message) {
        if (isPendingApprovalMail(message)) {
            return buildPendingApprovalEmailContent(message);
        }
        return buildApprovalResultEmailContent(message);
    }

    private String buildPendingApprovalEmailContent(NotificationMessage message) {
        String accent = resolveAccent(message);
        String template = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <style>
                    body{margin:0;padding:0;background:#edf2f7;font-family:'Microsoft YaHei','PingFang SC',sans-serif;color:#1e293b}
                    .page{padding:28px 12px;background:radial-gradient(circle at top left,#fff,#edf2f7 60%,#e2e8f0)}
                    .card{max-width:720px;margin:0 auto;background:#fff;border-radius:24px;overflow:hidden;box-shadow:0 24px 60px rgba(15,23,42,.12)}
                    .hero{padding:30px 34px 24px;color:#fff}.leave{background:linear-gradient(135deg,#0f766e,#14b8a6,#67e8f9)}.scholarship{background:linear-gradient(135deg,#9a3412,#f97316,#fdba74)}.grant{background:linear-gradient(135deg,#3730a3,#6366f1,#c4b5fd)}
                    .tag{display:inline-block;padding:6px 12px;border-radius:999px;background:rgba(255,255,255,.18);font-size:12px}.hero h1{margin:16px 0 10px;font-size:28px}.hero p{margin:0;line-height:1.8;color:rgba(255,255,255,.92)}
                    .main{padding:28px 34px 34px}.greet{margin:0 0 22px;line-height:1.9;color:#334155}.notice{padding:20px 22px;border-radius:20px;background:linear-gradient(180deg,#fefce8,#fef3c7);border:1px solid #fde68a;margin-bottom:22px;color:#92400e}
                    .panel{border:1px solid #e2e8f0;border-radius:20px;overflow:hidden;background:#f8fafc}.head{padding:16px 22px;font-weight:700;border-bottom:1px solid #e2e8f0}.head.leave{background:linear-gradient(90deg,rgba(20,184,166,.16),rgba(103,232,249,.05))}.head.scholarship{background:linear-gradient(90deg,rgba(249,115,22,.18),rgba(253,186,116,.05))}.head.grant{background:linear-gradient(90deg,rgba(99,102,241,.18),rgba(196,181,253,.05))}
                    .body{padding:8px 22px 18px}.row{display:flex;gap:16px;padding:14px 0;border-bottom:1px dashed #dbe4ee}.row:last-child{border-bottom:none}.k{width:108px;flex-shrink:0;color:#64748b}.v{flex:1;line-height:1.8;word-break:break-word}
                    .tip{margin-top:22px;padding:16px 18px;border-radius:16px;background:#eff6ff;border:1px solid #bfdbfe;color:#1d4ed8;line-height:1.8;font-size:13px}.foot{padding:18px 34px 30px;text-align:center;color:#94a3b8;font-size:12px;line-height:1.9}
                    @media(max-width:640px){.hero,.main,.foot{padding-left:20px;padding-right:20px}.row{display:block}.k{width:auto;margin-bottom:4px}}
                  </style>
                </head>
                <body>
                  <div class="page">
                    <div class="card">
                      <div class="hero __ACCENT__">
                        <span class="tag">校园智能服务平台</span>
                        <h1>__TITLE__</h1>
                        <p>您有新的审批任务待处理，请及时登录系统完成审核。</p>
                      </div>
                      <div class="main">
                        <p class="greet">尊敬的 __RECEIVER__，您好：<br>系统检测到有新的审批任务已流转至您，请尽快处理。</p>
                        <div class="notice">__CONTENT__</div>
                        __DETAIL__
                        <div class="tip">为避免流程超时，请尽快进入系统查看完整申请材料并执行审批操作。</div>
                      </div>
                      <div class="foot">Campus Workflow Mailer<br>校园智能服务平台 · 审批通知中心</div>
                    </div>
                  </div>
                </body>
                </html>
                """;

        return template
                .replace("__ACCENT__", accent)
                .replace("__TITLE__", escape(defaultText(message.getTitle(), "待审批任务提醒")))
                .replace("__RECEIVER__", escape(defaultText(message.getReceiverName(), "老师")))
                .replace("__CONTENT__", escape(defaultText(message.getContent(), "您有新的审批任务待处理。")))
                .replace("__DETAIL__", pendingDetail(message, accent));
    }

    private String buildApprovalResultEmailContent(NotificationMessage message) {
        String accent = resolveAccent(message);
        String status = "APPROVED".equals(message.getApprovalStatus()) ? "已通过" : "未通过";
        String statusClass = "APPROVED".equals(message.getApprovalStatus()) ? "ok" : "bad";

        String template = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <style>
                    body{margin:0;padding:0;background:#edf2f7;font-family:'Microsoft YaHei','PingFang SC',sans-serif;color:#1e293b}
                    .page{padding:28px 12px;background:radial-gradient(circle at top left,#fff,#edf2f7 60%,#e2e8f0)}
                    .card{max-width:720px;margin:0 auto;background:#fff;border-radius:24px;overflow:hidden;box-shadow:0 24px 60px rgba(15,23,42,.12)}
                    .hero{padding:30px 34px 24px;color:#fff}
                    .leave{background:linear-gradient(135deg,#0f766e,#14b8a6,#67e8f9)}
                    .scholarship{background:linear-gradient(135deg,#9a3412,#f97316,#fdba74)}
                    .grant{background:linear-gradient(135deg,#3730a3,#6366f1,#c4b5fd)}
                    .tag{display:inline-block;padding:6px 12px;border-radius:999px;background:rgba(255,255,255,.18);font-size:12px}
                    .hero h1{margin:16px 0 10px;font-size:28px}.hero p{margin:0;line-height:1.8;color:rgba(255,255,255,.92)}
                    .main{padding:28px 34px 34px}.greet{margin:0 0 22px;line-height:1.9;color:#334155}
                    .status{padding:20px 22px;border-radius:20px;background:linear-gradient(180deg,#f8fafc,#eef2ff);border:1px solid #e2e8f0;margin-bottom:22px}
                    .status .label{font-size:13px;color:#64748b}.status .value{font-size:24px;font-weight:700;margin:10px 0}.ok{color:#15803d}.bad{color:#dc2626}
                    .panel{border:1px solid #e2e8f0;border-radius:20px;overflow:hidden;background:#f8fafc}
                    .head{padding:16px 22px;font-weight:700;border-bottom:1px solid #e2e8f0}
                    .head.leave{background:linear-gradient(90deg,rgba(20,184,166,.16),rgba(103,232,249,.05))}
                    .head.scholarship{background:linear-gradient(90deg,rgba(249,115,22,.18),rgba(253,186,116,.05))}
                    .head.grant{background:linear-gradient(90deg,rgba(99,102,241,.18),rgba(196,181,253,.05))}
                    .body{padding:8px 22px 18px}.row{display:flex;gap:16px;padding:14px 0;border-bottom:1px dashed #dbe4ee}.row:last-child{border-bottom:none}
                    .k{width:108px;flex-shrink:0;color:#64748b}.v{flex:1;line-height:1.8;word-break:break-word}.tip{margin-top:22px;padding:16px 18px;border-radius:16px;background:#fff7ed;border:1px solid #fed7aa;color:#9a3412;line-height:1.8;font-size:13px}
                    .foot{padding:18px 34px 30px;text-align:center;color:#94a3b8;font-size:12px;line-height:1.9}
                    @media(max-width:640px){.hero,.main,.foot{padding-left:20px;padding-right:20px}.row{display:block}.k{width:auto;margin-bottom:4px}}
                  </style>
                </head>
                <body>
                  <div class="page">
                    <div class="card">
                      <div class="hero __ACCENT__">
                        <span class="tag">校园智能服务平台</span>
                        <h1>__TITLE__</h1>
                        <p>__SUMMARY__</p>
                      </div>
                      <div class="main">
                        <p class="greet">尊敬的 __APPLICANT__，您好：<br>您提交的事项已有新的审批结果，请查看下方详情。</p>
                        <div class="status">
                          <div class="label">当前审批状态</div>
                          <div class="value __STATUS_CLASS__">__STATUS__</div>
                          <div>__CONTENT__</div>
                        </div>
                        __DETAIL__
                        <div class="tip">此邮件由系统自动发送，请勿直接回复。如对审批结果有疑问，请及时联系辅导员或学工处老师。</div>
                      </div>
                      <div class="foot">Campus Workflow Mailer<br>校园智能服务平台 · 审批通知中心</div>
                    </div>
                  </div>
                </body>
                </html>
                """;

        return template
                .replace("__ACCENT__", accent)
                .replace("__TITLE__", escape(defaultText(message.getTitle(), "审批结果通知")))
                .replace("__SUMMARY__", escape(summary(message)))
                .replace("__APPLICANT__", escape(defaultText(message.getReceiverName(), defaultText(message.getApplicantName(), "同学"))))
                .replace("__STATUS_CLASS__", statusClass)
                .replace("__STATUS__", escape(status))
                .replace("__CONTENT__", escape(defaultText(message.getContent(), summary(message))))
                .replace("__DETAIL__", detail(message, accent, status, statusClass));
    }

    private boolean isPendingApprovalMail(NotificationMessage message) {
        return !"APPROVAL_RESULT".equals(message.getType());
    }

    private String detail(NotificationMessage message, String accent, String status, String statusClass) {
        if ("LEAVE".equals(message.getBusinessType())) {
            return panel("请假审批详情", accent,
                    row("申请人", defaultText(message.getApplicantName(), "-")) +
                    row("请假类型", defaultText(message.getAwardName(), "请假申请")) +
                    row("开始日期", formatDate(message.getLeaveStartDate())) +
                    row("结束日期", formatDate(message.getLeaveEndDate())) +
                    row("请假天数", message.getLeaveDays() == null ? "-" : message.getLeaveDays() + " 天") +
                    row("请假原因", defaultText(message.getLeaveReason(), "无")) +
                    row("业务编号", message.getBusinessId() == null ? "-" : "#" + message.getBusinessId()) +
                    row("审批状态", "<span class=\"" + statusClass + "\">" + ("ok".equals(statusClass) ? "已通过" : "未通过") + "</span>") +
                    row("审批意见", defaultText(message.getApprovalComment(), "无")) +
                    row("审批人", defaultText(message.getApproverName(), "系统")));
        }

        return panel(panelTitle(message), accent,
                row("申请人", defaultText(message.getApplicantName(), "-")) +
                row("申请类别", awardType(message.getApplicationType())) +
                row("项目名称", defaultText(message.getAwardName(), "-")) +
                row("申请金额", amount(message.getAmount())) +
                row("业务编号", message.getBusinessId() == null ? "-" : "#" + message.getBusinessId()) +
                row("审批状态", "<span class=\"" + statusClass + "\">" + escape(status) + "</span>") +
                row("审批意见", defaultText(message.getApprovalComment(), "无")) +
                row("审批人", defaultText(message.getApproverName(), "系统")));
    }

    private String pendingDetail(NotificationMessage message, String accent) {
        if ("LEAVE".equals(message.getBusinessType())) {
            return panel("待审批请假任务", accent,
                    row("申请人", defaultText(message.getApplicantName(), "-")) +
                    row("请假类型", defaultText(message.getAwardName(), "请假申请")) +
                    row("开始日期", formatDate(message.getLeaveStartDate())) +
                    row("结束日期", formatDate(message.getLeaveEndDate())) +
                    row("请假天数", message.getLeaveDays() == null ? "-" : message.getLeaveDays() + " 天") +
                    row("请假原因", defaultText(message.getLeaveReason(), "无")) +
                    row("业务编号", message.getBusinessId() == null ? "-" : "#" + message.getBusinessId()));
        }

        return panel("待审批奖助任务", accent,
                row("申请人", defaultText(message.getApplicantName(), "-")) +
                row("申请类别", awardType(message.getApplicationType())) +
                row("项目名称", defaultText(message.getAwardName(), "-")) +
                row("申请金额", amount(message.getAmount())) +
                row("业务编号", message.getBusinessId() == null ? "-" : "#" + message.getBusinessId()));
    }

    private String panel(String title, String accent, String body) {
        return "<div class=\"panel\"><div class=\"head " + accent + "\">" + escape(title) + "</div><div class=\"body\">" + body + "</div></div>";
    }

    private String row(String key, String value) {
        return "<div class=\"row\"><div class=\"k\">" + escape(key) + "</div><div class=\"v\">" + value + "</div></div>";
    }

    private String summary(NotificationMessage message) {
        if ("LEAVE".equals(message.getBusinessType())) {
            return "请假申请结果已更新，系统已为您整理审批状态与处理意见。";
        }
        return switch (message.getApplicationType()) {
            case "SCHOLARSHIP" -> "奖学金申请结果已更新，请及时查看评审与审批反馈。";
            case "GRANT" -> "助学金申请结果已更新，请及时查看审核与审批反馈。";
            case "SUBSIDY" -> "困难补助申请结果已更新，请及时查看审核与审批反馈。";
            default -> "您的申请结果已更新，请及时查看审批详情。";
        };
    }

    private String resolveAccent(NotificationMessage message) {
        if ("LEAVE".equals(message.getBusinessType())) {
            return "leave";
        }
        return switch (message.getApplicationType()) {
            case "SCHOLARSHIP" -> "scholarship";
            case "GRANT", "SUBSIDY" -> "grant";
            default -> "grant";
        };
    }

    private String panelTitle(NotificationMessage message) {
        return switch (message.getApplicationType()) {
            case "SCHOLARSHIP" -> "奖学金审批详情";
            case "GRANT" -> "助学金审批详情";
            case "SUBSIDY" -> "困难补助审批详情";
            default -> "奖助审批详情";
        };
    }

    private String awardType(String type) {
        return switch (type) {
            case "SCHOLARSHIP" -> "奖学金申请";
            case "GRANT" -> "助学金申请";
            case "SUBSIDY" -> "困难补助申请";
            default -> "奖助申请";
        };
    }

    private String amount(BigDecimal amount) {
        return amount == null ? "-" : "¥" + amount.stripTrailingZeros().toPlainString();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br>");
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
