package com.example.learnworkagent.infrastructure.config;

import com.example.learnworkagent.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket握手认证拦截器.
 * <p>在WebSocket连接建立时进行JWT认证校验.</p>
 *
 * @author system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response,
                                   @NotNull WebSocketHandler wsHandler, @NotNull Map<String, Object> attributes) {
        // 从URL参数中获取token
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            
            if (token != null && !token.isEmpty()) {
                try {
                    // 验证token并提取用户ID
                    if (jwtUtil.validateToken(token)) {
                        Long userId = jwtUtil.getUserIdFromToken(token);
                        attributes.put("userId", userId);
                        log.info("WebSocket认证成功，用户ID: {}", userId);
                        return true;
                    }
                } catch (Exception e) {
                    log.error("WebSocket认证失败: {}", e.getMessage());
                }
            }
        }
        
        log.warn("WebSocket连接缺少有效token");
        return true; // 允许连接，但用户未认证
    }

    @Override
    public void afterHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response,
                               @NotNull WebSocketHandler wsHandler, Exception exception) {
        // 握手后的处理
    }
}
