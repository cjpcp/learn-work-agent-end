package com.example.learnworkagent.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Objects;

/**
 * WebSocket消息频道拦截器.
 * <p>在STOMP消息处理时设置用户身份信息.</p>
 *
 * @author system
 */
@Slf4j
@Component
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 从握手时设置的attributes中获取userId
            Object userId = Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
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
        private record StompPrincipal(String name) implements Principal {

        @Override
            public String getName() {
                return name;
            }
        }
}
