package com.deskpilot.relay.config;

import com.deskpilot.relay.relay.handler.RelayWebSocketHandler;
import com.deskpilot.relay.security.jwt.JwtService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RelayWebSocketHandler relayWebSocketHandler;
    private final JwtService jwtService;

    public WebSocketConfig(RelayWebSocketHandler relayWebSocketHandler,
                           JwtService jwtService) {
        this.relayWebSocketHandler = relayWebSocketHandler;
        this.jwtService = jwtService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(relayWebSocketHandler, "/relay")
                .addInterceptors(new JwtHandshakeInterceptor(jwtService))
                .setAllowedOrigins("*");
    }
}