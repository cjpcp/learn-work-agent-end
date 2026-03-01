package com.example.learnworkagent.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * WebSocket Channel拦截器
 * 用于在STOMP消息处理时设置用户身份
 */
@Slf4j
@Component
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 从握手时设置的attributes中获取userId
            Object userId = accessor.getSessionAttributes().get("userId");
            log.info("STOMP CONNECT - userId from session: {}", userId);
            if (userId != null) {
                // 设置用户Principal
                accessor.setUser(new StompPrincipal(userId.toString()));
                log.info("STOMP CONNECT - 已设置用户Principal: {}", userId);
            } else {
                log.warn("STOMP CONNECT - userId为空，无法设置用户Principal");
            }
        }
        
        return message;
    }

    /**
     * 简单的Principal实现
     */
    private static class StompPrincipal implements Principal {
        private final String name;

        public StompPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
