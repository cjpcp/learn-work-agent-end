package com.example.learnworkagent.infrastructure.external.notification;

import com.example.learnworkagent.common.enums.ApprovalStatusEnum;
import com.example.learnworkagent.common.enums.NotificationChannelEnum;
import com.example.learnworkagent.common.enums.NotificationTypeEnum;
import com.example.learnworkagent.domain.notification.entity.NotificationMessage;
import com.example.learnworkagent.domain.notification.service.NotificationSender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 微信公众号模板消息通知发送器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatMpNotificationSender implements NotificationSender {

    private static final String WX_API_BASE = "https://api.weixin.qq.com";
    private static final String CHANNEL_WECHAT_MP = NotificationChannelEnum.WECHAT_MP.getCode();
    private static final String NOTIFICATION_TYPE_APPROVAL_RESULT = NotificationTypeEnum.APPROVAL_RESULT.getCode();
    private static final String ACCESS_TOKEN_CACHE_KEY = "wechat:mp:access_token";
    private static final long ACCESS_TOKEN_EXPIRE_SECONDS = 6900L;
    private static final String DEFAULT_TITLE = "审批结果通知";
    private static final String DEFAULT_APPROVAL_COMMENT = "无";
    private static final String DEFAULT_APPROVER_NAME = "系统";
    private static final String DEFAULT_REMARK = "如有疑问，请登录系统或联系学工处咨询。";
    private static final String DEFAULT_TEXT_COLOR = "#333333";
    private static final String SUCCESS_COLOR = "#00AA00";
    private static final String FAIL_COLOR = "#FF0000";
    private static final String REMARK_COLOR = "#999999";
    private static final String DEFAULT_APPLICANT_NAME = "";
    private static final String SUCCESS_RESPONSE_TEXT = "\"errcode\":0";

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Value("${wechat.mp.app-id:}")
    private String appId;

    @Value("${wechat.mp.app-secret:}")
    private String appSecret;

    @Value("${wechat.mp.template-id:}")
    private String templateId;

    @Value("${wechat.mp.redirect-url:}")
    private String redirectUrl;

    @Value("${wechat.mp.mock.enabled:true}")
    private boolean mockEnabled;

    @Override
    public String getChannel() {
        return CHANNEL_WECHAT_MP;
    }

    @Override
    public boolean send(NotificationMessage message) {
        if (hasBlank(appId) || hasBlank(appSecret)) {
            log.warn("微信公众号 AppID/AppSecret 未配置，跳过推送，用户ID: {}", message.getUserId());
            return false;
        }

        String openId = message.getWechatOpenId();
        if (hasBlank(openId)) {
            log.warn("用户未绑定微信OpenID，跳过公众号推送，用户ID: {}", message.getUserId());
            return false;
        }

        if (hasBlank(templateId)) {
            log.warn("微信公众号模板ID未配置，跳过推送，用户ID: {}", message.getUserId());
            return false;
        }

        try {
            if (mockEnabled) {
                logMockMessage(message, openId);
                return true;
            }

            String accessToken = getAccessToken();
            if (accessToken == null) {
                log.error("获取微信 AccessToken 失败，无法推送公众号消息，用户ID: {}", message.getUserId());
                return false;
            }

            String response = webClientBuilder.build()
                    .post()
                    .uri(WX_API_BASE + "/cgi-bin/message/template/send?access_token=" + accessToken)
                    .header("Content-Type", "application/json")
                    .bodyValue(objectMapper.writeValueAsString(buildTemplateMessageBody(openId, message)))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("微信公众号推送响应: {}", response);
            boolean success = isSuccessResponse(response);
            log.info("微信公众号模板消息推送{}，用户ID: {}", success ? "成功" : "失败", message.getUserId());
            return success;
        } catch (Exception exception) {
            log.error("微信公众号推送异常，用户ID: {}", message.getUserId(), exception);
            return false;
        }
    }

    private Map<String, Object> buildTemplateMessageBody(String openId, NotificationMessage message) {
        Map<String, Object> data = new HashMap<>(6);
        data.put("first", textItem(resolveTitle(message), DEFAULT_TEXT_COLOR));
        data.put("keyword1", textItem(defaultText(message.getApplicantName(), DEFAULT_APPLICANT_NAME), DEFAULT_TEXT_COLOR));
        data.put("keyword2", textItem(resolveStatusText(message), resolveStatusColor(message)));
        data.put("keyword3", textItem(defaultText(message.getApprovalComment(), DEFAULT_APPROVAL_COMMENT), DEFAULT_TEXT_COLOR));
        data.put("keyword4", textItem(defaultText(message.getApproverName(), DEFAULT_APPROVER_NAME), DEFAULT_TEXT_COLOR));
        data.put("remark", textItem(resolveRemark(message), REMARK_COLOR));

        Map<String, Object> body = new HashMap<>(4);
        body.put("touser", openId);
        body.put("template_id", templateId);
        body.put("data", data);
        if (!hasBlank(redirectUrl)) {
            body.put("url", redirectUrl);
        }
        return body;
    }

    private Map<String, String> textItem(String value, String color) {
        Map<String, String> item = new HashMap<>(2);
        item.put("value", value);
        item.put("color", color);
        return item;
    }

    private String getAccessToken() {
        String cachedToken = redisTemplate.opsForValue().get(ACCESS_TOKEN_CACHE_KEY);
        if (!hasBlank(cachedToken)) {
            return cachedToken;
        }

        try {
            String response = webClientBuilder.build()
                    .get()
                    .uri(WX_API_BASE + "/cgi-bin/token?grant_type=client_credential&appid=" + appId + "&secret=" + appSecret)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) {
                return null;
            }

            JsonNode node = objectMapper.readTree(response);
            if (!node.has("access_token")) {
                log.error("获取微信 AccessToken 失败，响应: {}", response);
                return null;
            }

            String token = node.get("access_token").asText();
            redisTemplate.opsForValue().set(ACCESS_TOKEN_CACHE_KEY, token, ACCESS_TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
            log.info("微信公众号 AccessToken 刷新成功");
            return token;
        } catch (Exception exception) {
            log.error("请求微信 AccessToken 异常", exception);
            return null;
        }
    }

    private String resolveTitle(NotificationMessage message) {
        return defaultText(message.getTitle(), DEFAULT_TITLE);
    }

    private String resolveStatusText(NotificationMessage message) {
        return isApprovalResultNotification(message) && isApproved(message) ? "已通过" : "未通过";
    }

    private String resolveStatusColor(NotificationMessage message) {
        return isApprovalResultNotification(message) && isApproved(message) ? SUCCESS_COLOR : FAIL_COLOR;
    }

    private String resolveRemark(NotificationMessage message) {
        return isApprovalResultNotification(message)
                ? DEFAULT_REMARK
                : defaultText(message.getContent(), DEFAULT_REMARK);
    }

    private boolean isApprovalResultNotification(NotificationMessage message) {
        return NOTIFICATION_TYPE_APPROVAL_RESULT.equals(message.getType());
    }

    private boolean isApproved(NotificationMessage message) {
        return ApprovalStatusEnum.APPROVED.getCode().equals(message.getApprovalStatus());
    }

    private boolean isSuccessResponse(String response) {
        return response != null && response.contains(SUCCESS_RESPONSE_TEXT);
    }

    private void logMockMessage(NotificationMessage message, String openId) {
        log.info("【微信公众号模拟推送】用户ID: {}, OpenID: {}, 标题: {}, 状态: {}",
                message.getUserId(), maskOpenId(openId), resolveTitle(message), resolveStatusText(message));
    }

    private String defaultText(String value, String defaultValue) {
        return hasBlank(value) ? defaultValue : value;
    }

    private boolean hasBlank(String value) {
        return value == null || value.isBlank();
    }

    private String maskOpenId(String openId) {
        if (hasBlank(openId) || openId.length() <= 6) {
            return "***";
        }
        return openId.substring(0, 3) + "***" + openId.substring(openId.length() - 3);
    }
}
