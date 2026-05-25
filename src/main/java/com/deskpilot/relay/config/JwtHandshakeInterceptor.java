package com.deskpilot.relay.config;

import com.deskpilot.relay.security.jwt.JwtService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    public JwtHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        // Tenta extrair token do header Authorization
        List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.get(0);
            if (header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    String username = jwtService.extractUsername(token);
                    if (username != null && jwtService.isTokenValid(token, username)) {
                        attributes.put("username", username);
                        return true;
                    }
                } catch (Exception ignored) {}
            }
        }

        // Tenta extrair token via query param ?token=xxx
        // (útil para clientes que não conseguem setar headers no WS)
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            String token = extractTokenFromQuery(query);
            try {
                String username = jwtService.extractUsername(token);
                if (username != null && jwtService.isTokenValid(token, username)) {
                    attributes.put("username", username);
                    return true;
                }
            } catch (Exception ignored) {}
        }

        // Token inválido ou ausente — recusa o handshake
        return false;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // nada a fazer após o handshake
    }

    private String extractTokenFromQuery(String query) {
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                return param.substring(6);
            }
        }
        return "";
    }
}