package com.example.learnworkagent.infrastructure.external.notification;

import com.example.learnworkagent.domain.notification.entity.NotificationMessage;
import com.example.learnworkagent.domain.notification.service.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * 短信通知发送器
 * 注意：由于阿里云短信服务不支持个人用户，此发送器暂时禁用
 * 如需启用，请取消 @Component 注解的注释，并配置短信服务参数
 */
@Slf4j
// @Component  // 暂时注释掉，不启用短信发送功能
@RequiredArgsConstructor
public class SmsNotificationSender implements NotificationSender {

    @Value("${sms.provider:aliyun}")
    private String smsProvider;

    @Value("${sms.aliyun.access-key:}")
    private String aliyunAccessKey;

    @Value("${sms.aliyun.access-secret:}")
    private String aliyunAccessSecret;

    @Value("${sms.aliyun.sign-name:}")
    private String aliyunSignName;

    @Value("${sms.aliyun.template-code:}")
    private String aliyunTemplateCode;

    @Value("${sms.mock.enabled:true}")
    private boolean mockEnabled;

    @Override
    public String getChannel() {
        return "SMS";
    }

    @Override
    public boolean send(NotificationMessage message) {
        if (message.getPhone() == null || message.getPhone().isEmpty()) {
            log.warn("用户手机号为空，无法发送短信通知，用户ID: {}", message.getUserId());
            return false;
        }

        try {
            log.info("发送短信通知到: {}, 内容: {}", maskPhone(message.getPhone()), message.getTitle());

            if (mockEnabled) {
                // 模拟发送，记录日志
                log.info("【短信模拟发送】手机号: {}, 内容: {}",
                        maskPhone(message.getPhone()), buildSmsContent(message));
                return true;
            }

            // 实际发送短信（这里以阿里云短信为例）
            return sendAliyunSms(message);

        } catch (Exception e) {
            log.error("短信通知发送失败: {}", maskPhone(message.getPhone()), e);
            return false;
        }
    }

    /**
     * 发送阿里云短信
     */
    private boolean sendAliyunSms(NotificationMessage message) {
        // 这里实现阿里云短信发送逻辑
        // 实际项目中需要引入阿里云短信SDK
        // 示例代码：
        // DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", aliyunAccessKey, aliyunAccessSecret);
        // IAcsClient client = new DefaultAcsClient(profile);
        // SendSmsRequest request = new SendSmsRequest();
        // request.setPhoneNumbers(message.getPhone());
        // request.setSignName(aliyunSignName);
        // request.setTemplateCode(aliyunTemplateCode);
        // request.setTemplateParam(buildTemplateParam(message));
        // SendSmsResponse response = client.getAcsResponse(request);
        // return "OK".equals(response.getCode());

        log.info("阿里云短信发送功能待配置，手机号: {}", maskPhone(message.getPhone()));
        return true;
    }

    /**
     * 构建短信内容
     */
    private String buildSmsContent(NotificationMessage message) {
        String status = "APPROVED".equals(message.getApprovalStatus()) ? "已通过" : "未通过";
        return String.format("【校园智能服务】%s您好，您的%s（%s）申请%s。审批意见：%s",
                message.getApplicantName(),
                getApplicationTypeName(message.getApplicationType()),
                message.getAwardName(),
                status,
                message.getApprovalComment());
    }

    /**
     * 构建短信模板参数（JSON格式）
     */
    private String buildTemplateParam(NotificationMessage message) {
        String status = "APPROVED".equals(message.getApprovalStatus()) ? "已通过" : "未通过";
        return String.format("{\"name\":\"%s\",\"type\":\"%s\",\"award\":\"%s\",\"status\":\"%s\",\"comment\":\"%s\"}",
                message.getApplicantName(),
                getApplicationTypeName(message.getApplicationType()),
                message.getAwardName(),
                status,
                message.getApprovalComment());
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
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
