package com.example.learnworkagent.infrastructure.external.notification;

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
 * 企业微信应用消息通知发送器
 * 通过企业微信「应用消息」接口向指定成员推送消息
 * 文档：https://developer.work.weixin.qq.com/document/path/90236
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeworkNotificationSender implements NotificationSender {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String WEWORK_API_BASE = "https://qyapi.weixin.qq.com";
    private static final String ACCESS_TOKEN_CACHE_KEY = "wework:app:access_token";

    /** 企业ID，企业微信管理后台 -> 我的企业 -> 企业信息 */
    @Value("${wework.app.corp-id:}")
    private String corpId;

    /** 应用 Secret，企业微信管理后台 -> 应用管理 -> 对应应用 -> Secret */
    @Value("${wework.app.corp-secret:}")
    private String corpSecret;

    /** 应用 AgentID，企业微信管理后台 -> 应用管理 -> 对应应用 -> AgentId */
    @Value("${wework.app.agent-id:}")
    private String agentId;

    @Value("${wework.app.mock.enabled:true}")
    private boolean mockEnabled;

    @Override
    public String getChannel() {
        return "WEWORK";
    }

    @Override
    public boolean send(NotificationMessage message) {
        if (corpId == null || corpId.isBlank()
                || corpSecret == null || corpSecret.isBlank()
                || agentId == null || agentId.isBlank()) {
            log.warn("企业微信应用配置不完整（corpId/corpSecret/agentId），跳过推送，用户ID: {}", message.getUserId());
            return false;
        }

        String weworkUserId = message.getWeworkUserId();
        if (weworkUserId == null || weworkUserId.isBlank()) {
            log.warn("用户未绑定企业微信 UserId，跳过应用消息推送，用户ID: {}", message.getUserId());
            return false;
        }

        try {
            String content = buildTextCardContent(message);

            if (mockEnabled) {
                log.info("【企业微信应用消息模拟推送】用户ID: {}, 企微UserId: {}\n内容: {}",
                        message.getUserId(), weworkUserId, content);
                return true;
            }

            String accessToken = getAccessToken();
            if (accessToken == null) {
                log.error("获取企业微信 AccessToken 失败，无法推送应用消息，用户ID: {}", message.getUserId());
                return false;
            }

            // 构造文本卡片消息体
            Map<String, Object> textCard = new HashMap<>();
            textCard.put("title", message.getTitle() != null ? message.getTitle() : "审批结果通知");
            textCard.put("description", content);
            textCard.put("url", "https://work.weixin.qq.com"); // 可替换为系统实际跳转URL
            textCard.put("btntxt", "查看详情");

            Map<String, Object> body = new HashMap<>();
            body.put("touser", weworkUserId);   // 收件人企业微信账号，多人用 | 分隔
            body.put("msgtype", "textcard");
            body.put("agentid", Integer.parseInt(agentId));
            body.put("textcard", textCard);

            String url = WEWORK_API_BASE + "/cgi-bin/message/send?access_token=" + accessToken;
            String response = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .bodyValue(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("企业微信应用消息推送响应: {}", response);

            // 成功响应: {"errcode":0,"errmsg":"ok",...}
            boolean success = response != null && response.contains("\"errcode\":0");
            log.info("企业微信应用消息推送{}，用户ID: {}", success ? "成功" : "失败", message.getUserId());
            return success;

        } catch (Exception e) {
            log.error("企业微信应用消息推送异常，用户ID: {}", message.getUserId(), e);
            return false;
        }
    }

    /**
     * 构建文本卡片 description 内容（支持简单 HTML）
     */
    private String buildTextCardContent(NotificationMessage message) {
        String statusText = "APPROVED".equals(message.getApprovalStatus()) ? "已通过" : "未通过";
        String statusColor = "APPROVED".equals(message.getApprovalStatus())
                ? "<font color=\"info\">" : "<font color=\"warning\">";

        String applicantName = message.getApplicantName() != null ? message.getApplicantName() : "";
        String approvalComment = message.getApprovalComment() != null ? message.getApprovalComment() : "无";
        String approverName = message.getApproverName() != null ? message.getApproverName() : "系统";

        StringBuilder sb = new StringBuilder();
        sb.append("申请人：").append(applicantName).append("<br/>");
        sb.append("审批状态：").append(statusColor).append(statusText).append("</font><br/>");
        if (message.getAwardName() != null) {
            sb.append("奖项名称：").append(message.getAwardName()).append("<br/>");
        }
        if (message.getAmount() != null) {
            sb.append("申请金额：¥").append(message.getAmount()).append("<br/>");
        }
        sb.append("审批意见：").append(approvalComment).append("<br/>");
        sb.append("审批人：").append(approverName);
        return sb.toString();
    }

    /**
     * 获取企业微信应用 AccessToken（带 Redis 缓存，有效期 7200s，缓存 6900s）
     */
    private String getAccessToken() {
        String cached = redisTemplate.opsForValue().get(ACCESS_TOKEN_CACHE_KEY);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        try {
            String url = WEWORK_API_BASE + "/cgi-bin/gettoken?corpid=" + corpId + "&corpsecret=" + corpSecret;
            String response = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) return null;

            JsonNode node = objectMapper.readTree(response);
            if (node.has("access_token")) {
                String token = node.get("access_token").asText();
                redisTemplate.opsForValue().set(ACCESS_TOKEN_CACHE_KEY, token, 6900, TimeUnit.SECONDS);
                log.info("企业微信应用 AccessToken 刷新成功");
                return token;
            } else {
                log.error("获取企业微信 AccessToken 失败，响应: {}", response);
                return null;
            }
        } catch (Exception e) {
            log.error("请求企业微信 AccessToken 异常", e);
            return null;
        }
    }
}
