package com.incidentiq.notification.config;

import com.incidentiq.auth.security.UserPrincipal;
import com.incidentiq.auth.service.JwtService;
import com.incidentiq.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.UUID;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private final JwtService jwtService;

    public WebSocketConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (!StringUtils.hasText(authHeader)) {
                        authHeader = accessor.getFirstNativeHeader("token");
                    }

                    if (StringUtils.hasText(authHeader)) {
                        String token = authHeader.startsWith("Bearer ")
                                ? authHeader.substring(7)
                                : authHeader;

                        try {
                            if (jwtService.isTokenValid(token)) {
                                Claims claims = jwtService.parseAndValidateToken(token);
                                UUID userId = UUID.fromString(claims.getSubject());
                                String roleStr = claims.get("role", String.class);
                                String name = claims.get("name", String.class);
                                String email = claims.get("email", String.class);
                                Boolean mustChangePassword = claims.get("mustChangePassword", Boolean.class);

                                UserRole role = UserRole.valueOf(roleStr);
                                UserPrincipal principal = new UserPrincipal(
                                        userId,
                                        email,
                                        name,
                                        "",
                                        role,
                                        true,
                                        Boolean.TRUE.equals(mustChangePassword)
                                );

                                UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                                accessor.setUser(auth);
                                log.debug("Authenticated STOMP connection for user {} ({})", name, email);
                            } else {
                                log.warn("Invalid JWT presented in STOMP CONNECT frame");
                            }
                        } catch (Exception e) {
                            log.warn("STOMP JWT parsing failed: {}", e.getMessage());
                        }
                    }
                }

                return message;
            }
        });
    }
}
