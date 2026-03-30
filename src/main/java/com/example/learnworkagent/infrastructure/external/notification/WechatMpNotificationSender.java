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
 * 微信公众号模板消息通知发送器
 * 通过微信公众号模板消息接口向关注用户推送审批结果
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatMpNotificationSender implements NotificationSender {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String WX_API_BASE = "https://api.weixin.qq.com";
    private static final String ACCESS_TOKEN_CACHE_KEY = "wechat:mp:access_token";

    @Value("${wechat.mp.app-id:}")
    private String appId;

    @Value("${wechat.mp.app-secret:}")
    private String appSecret;

    /**
     * 模板消息模板ID（需在公众号后台配置好并填入）
     */
    @Value("${wechat.mp.template-id:}")
    private String templateId;

    /**
     * 点击消息后跳转的页面URL（可选）
     */
    @Value("${wechat.mp.redirect-url:}")
    private String redirectUrl;

    @Value("${wechat.mp.mock.enabled:true}")
    private boolean mockEnabled;

    @Override
    public String getChannel() {
        return "WECHAT_MP";
    }

    @Override
    public boolean send(NotificationMessage message) {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            log.warn("微信公众号 AppID/AppSecret 未配置，跳过推送，用户ID: {}", message.getUserId());
            return false;
        }

        String openId = message.getWechatOpenId();
        if (openId == null || openId.isBlank()) {
            log.warn("用户未绑定微信OpenID，跳过公众号推送，用户ID: {}", message.getUserId());
            return false;
        }

        if (templateId == null || templateId.isBlank()) {
            log.warn("微信公众号模板ID未配置，跳过推送，用户ID: {}", message.getUserId());
            return false;
        }

        try {
            if (mockEnabled) {
                log.info("【微信公众号模拟推送】用户ID: {}, OpenID: {}, 标题: {}, 状态: {}",
                        message.getUserId(), maskOpenId(openId),
                        message.getTitle(), message.getApprovalStatus());
                return true;
            }

            String accessToken = getAccessToken();
            if (accessToken == null) {
                log.error("获取微信 AccessToken 失败，无法推送公众号消息，用户ID: {}", message.getUserId());
                return false;
            }

            // 构造模板消息请求体
            Map<String, Object> body = buildTemplateMessageBody(openId, message);

            String url = WX_API_BASE + "/cgi-bin/message/template/send?access_token=" + accessToken;
            String response = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .bodyValue(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("微信公众号推送响应: {}", response);

            // 微信成功响应: {"errcode":0,"errmsg":"ok",...}
            boolean success = response != null && response.contains("\"errcode\":0");
            log.info("微信公众号模板消息推送{}，用户ID: {}", success ? "成功" : "失败", message.getUserId());
            return success;

        } catch (Exception e) {
            log.error("微信公众号推送异常，用户ID: {}", message.getUserId(), e);
            return false;
        }
    }

    /**
     * 构造模板消息请求体
     * 模板变量说明（与公众号后台模板对应）：
     *   {{first.DATA}}    — 消息标题
     *   {{keyword1.DATA}} — 申请人姓名
     *   {{keyword2.DATA}} — 审批状态
     *   {{keyword3.DATA}} — 审批意见
     *   {{keyword4.DATA}} — 审批人
     *   {{remark.DATA}}   — 备注提示
     */
    private Map<String, Object> buildTemplateMessageBody(String openId, NotificationMessage message) {
        String statusText = "APPROVED".equals(message.getApprovalStatus()) ? "已通过" : "未通过";
        String statusColor = "APPROVED".equals(message.getApprovalStatus()) ? "#00AA00" : "#FF0000";

        Map<String, Object> data = new HashMap<>();
        data.put("first",    textItem(message.getTitle() != null ? message.getTitle() : "审批结果通知", "#333333"));
        data.put("keyword1", textItem(message.getApplicantName() != null ? message.getApplicantName() : "", "#333333"));
        data.put("keyword2", textItem(statusText, statusColor));
        data.put("keyword3", textItem(message.getApprovalComment() != null ? message.getApprovalComment() : "无", "#333333"));
        data.put("keyword4", textItem(message.getApproverName() != null ? message.getApproverName() : "系统", "#333333"));
        data.put("remark",   textItem("如有疑问，请登录系统或联系学工处咨询。", "#999999"));

        Map<String, Object> body = new HashMap<>();
        body.put("touser", openId);
        body.put("template_id", templateId);
        body.put("data", data);
        if (redirectUrl != null && !redirectUrl.isBlank()) {
            body.put("url", redirectUrl);
        }
        return body;
    }

    private Map<String, String> textItem(String value, String color) {
        Map<String, String> item = new HashMap<>();
        item.put("value", value);
        item.put("color", color);
        return item;
    }

    /**
     * 获取微信公众号 AccessToken（带 Redis 缓存，有效期 7200s，提前 300s 刷新）
     */
    private String getAccessToken() {
        // 先从 Redis 缓存读取
        String cached = redisTemplate.opsForValue().get(ACCESS_TOKEN_CACHE_KEY);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }

        try {
            String url = WX_API_BASE + "/cgi-bin/token?grant_type=client_credential&appid="
                    + appId + "&secret=" + appSecret;

            String response = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) {
                return null;
            }

            JsonNode node = objectMapper.readTree(response);
            if (node.has("access_token")) {
                String token = node.get("access_token").asText();
                // 微信token有效期7200s，缓存6900s
                redisTemplate.opsForValue().set(ACCESS_TOKEN_CACHE_KEY, token, 6900, TimeUnit.SECONDS);
                log.info("微信公众号 AccessToken 刷新成功");
                return token;
            } else {
                log.error("获取微信 AccessToken 失败，响应: {}", response);
                return null;
            }
        } catch (Exception e) {
            log.error("请求微信 AccessToken 异常", e);
            return null;
        }
    }

    private String maskOpenId(String openId) {
        if (openId == null || openId.length() <= 6) return "***";
        return openId.substring(0, 3) + "***" + openId.substring(openId.length() - 3);
    }
}
