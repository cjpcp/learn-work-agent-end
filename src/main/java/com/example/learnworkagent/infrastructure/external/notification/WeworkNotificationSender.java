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
 * 企业微信应用消息通知发送器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeworkNotificationSender implements NotificationSender {

    private static final String WEWORK_API_BASE = "https://qyapi.weixin.qq.com";
    private static final String CHANNEL_WEWORK = NotificationChannelEnum.WEWORK.getCode();
    private static final String NOTIFICATION_TYPE_APPROVAL_RESULT = NotificationTypeEnum.APPROVAL_RESULT.getCode();
    private static final String ACCESS_TOKEN_CACHE_KEY = "wework:app:access_token";
    private static final String DEFAULT_TITLE = "审批结果通知";
    private static final String DEFAULT_APPROVAL_COMMENT = "无";
    private static final String DEFAULT_APPROVER_NAME = "系统";
    private static final String DETAIL_BUTTON_TEXT = "查看详情";
    private static final String DETAIL_URL = "https://work.weixin.qq.com";
    private static final long ACCESS_TOKEN_EXPIRE_SECONDS = 6900L;
    private static final String DEFAULT_APPLICANT_NAME = "";
    private static final String SUCCESS_RESPONSE_TEXT = "\"errcode\":0";

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Value("${wework.app.corp-id:}")
    private String corpId;

    @Value("${wework.app.corp-secret:}")
    private String corpSecret;

    @Value("${wework.app.agent-id:}")
    private String agentId;

    @Value("${wework.app.mock.enabled:true}")
    private boolean mockEnabled;

    @Override
    public String getChannel() {
        return CHANNEL_WEWORK;
    }

    @Override
    public boolean send(NotificationMessage message) {
        if (hasBlank(corpId) || hasBlank(corpSecret) || hasBlank(agentId)) {
            log.warn("企业微信应用配置不完整（corpId/corpSecret/agentId），跳过推送，用户ID: {}", message.getUserId());
            return false;
        }

        String weworkUserId = message.getWeworkUserId();
        if (hasBlank(weworkUserId)) {
            log.warn("用户未绑定企业微信 UserId，跳过应用消息推送，用户ID: {}", message.getUserId());
            return false;
        }

        try {
            String content = buildTextCardContent(message);
            if (mockEnabled) {
                logMockMessage(message, weworkUserId, content);
                return true;
            }

            String accessToken = getAccessToken();
            if (accessToken == null) {
                log.error("获取企业微信 AccessToken 失败，无法推送应用消息，用户ID: {}", message.getUserId());
                return false;
            }

            String response = webClientBuilder.build()
                    .post()
                    .uri(WEWORK_API_BASE + "/cgi-bin/message/send?access_token=" + accessToken)
                    .header("Content-Type", "application/json")
                    .bodyValue(objectMapper.writeValueAsString(buildMessageBody(weworkUserId, content, resolveTitle(message))))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("企业微信应用消息推送响应: {}", response);
            boolean success = isSuccessResponse(response);
            log.info("企业微信应用消息推送{}，用户ID: {}", success ? "成功" : "失败", message.getUserId());
            return success;
        } catch (Exception exception) {
            log.error("企业微信应用消息推送异常，用户ID: {}", message.getUserId(), exception);
            return false;
        }
    }

    private Map<String, Object> buildMessageBody(String weworkUserId, String content, String title) {
        Map<String, Object> textCard = new HashMap<>(4);
        textCard.put("title", title);
        textCard.put("description", content);
        textCard.put("url", DETAIL_URL);
        textCard.put("btntxt", DETAIL_BUTTON_TEXT);

        Map<String, Object> body = new HashMap<>(4);
        body.put("touser", weworkUserId);
        body.put("msgtype", "textcard");
        body.put("agentid", Integer.parseInt(agentId));
        body.put("textcard", textCard);
        return body;
    }

    private String buildTextCardContent(NotificationMessage message) {
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("申请人：").append(defaultText(message.getApplicantName(), DEFAULT_APPLICANT_NAME)).append("<br/>");
        contentBuilder.append("审批状态：")
                .append(resolveStatusColor(message))
                .append(resolveStatusText(message))
                .append("</font><br/>");
        appendIfPresent(contentBuilder, message.getAwardName());
        if (message.getAmount() != null) {
            contentBuilder.append("申请金额：¥").append(message.getAmount()).append("<br/>");
        }
        contentBuilder.append("审批意见：")
                .append(defaultText(message.getApprovalComment(), DEFAULT_APPROVAL_COMMENT))
                .append("<br/>");
        contentBuilder.append("审批人：")
                .append(defaultText(message.getApproverName(), DEFAULT_APPROVER_NAME));
        return contentBuilder.toString();
    }

    private void appendIfPresent(StringBuilder contentBuilder, String value) {
        if (!hasBlank(value)) {
            contentBuilder.append("奖项名称：").append(value).append("<br/>");
        }
    }

    private String getAccessToken() {
        String cachedToken = redisTemplate.opsForValue().get(ACCESS_TOKEN_CACHE_KEY);
        if (!hasBlank(cachedToken)) {
            return cachedToken;
        }

        try {
            String response = webClientBuilder.build()
                    .get()
                    .uri(WEWORK_API_BASE + "/cgi-bin/gettoken?corpid=" + corpId + "&corpsecret=" + corpSecret)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) {
                return null;
            }

            JsonNode node = objectMapper.readTree(response);
            if (!node.has("access_token")) {
                log.error("获取企业微信 AccessToken 失败，响应: {}", response);
                return null;
            }

            String token = node.get("access_token").asText();
            redisTemplate.opsForValue().set(ACCESS_TOKEN_CACHE_KEY, token, ACCESS_TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
            log.info("企业微信应用 AccessToken 刷新成功");
            return token;
        } catch (Exception exception) {
            log.error("请求企业微信 AccessToken 异常", exception);
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
        return isApprovalResultNotification(message) && isApproved(message)
                ? "<font color=\"info\">"
                : "<font color=\"warning\">";
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

    private void logMockMessage(NotificationMessage message, String weworkUserId, String content) {
        log.info("【企业微信应用消息模拟推送】用户ID: {}, 企微UserId: {}\n标题: {}\n内容: {}",
                message.getUserId(), weworkUserId, resolveTitle(message), content);
    }

    private String defaultText(String value, String defaultValue) {
        return hasBlank(value) ? defaultValue : value;
    }

    private boolean hasBlank(String value) {
        return value == null || value.isBlank();
    }
}
