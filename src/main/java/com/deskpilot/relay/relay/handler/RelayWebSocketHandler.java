package com.deskpilot.relay.relay.handler;

import tools.jackson.databind.ObjectMapper;
import com.deskpilot.relay.relay.model.CommandType;
import com.deskpilot.relay.relay.model.RelayMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RelayWebSocketHandler extends AbstractWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Sessões ativas: username → sessão WebSocket
    private final Map<String, WebSocketSession> agentSessions  = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> clientSessions = new ConcurrentHashMap<>();

    // ─── Conexão estabelecida ────────────────────────────────────────────────────

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String username = (String) session.getAttributes().get("username");
        String role     = getRole(session);

        if ("agent".equals(role)) {
            agentSessions.put(username, session);
            notifyClient(username, CommandType.AGENT_CONNECTED, "Agente conectado");
        } else {
            clientSessions.put(username, session);
            notifyAgent(username, CommandType.CLIENT_CONNECTED, "Cliente conectado");
        }
    }

    // ─── Mensagem de texto recebida ──────────────────────────────────────────────

    @Override
    protected void handleTextMessage(WebSocketSession session,
                                     TextMessage message) throws Exception {
        String username = (String) session.getAttributes().get("username");
        String role     = getRole(session);
        RelayMessage relay = objectMapper.readValue(message.getPayload(), RelayMessage.class);

        // PING → responde PONG direto
        if (relay.getType() == CommandType.PING) {
            sendText(session, RelayMessage.builder()
                    .type(CommandType.PONG)
                    .timestamp(System.currentTimeMillis())
                    .build());
            return;
        }

        // Cliente envia comando → repassa para o agente
        if ("client".equals(role)) {
            forwardToAgent(username, relay);
            return;
        }

        // Agente envia evento → repassa para o cliente
        if ("agent".equals(role)) {
            forwardToClient(username, relay);
        }
    }

    // ─── Mensagem binária (frames de tela) ──────────────────────────────────────

    @Override
    protected void handleBinaryMessage(WebSocketSession session,
                                       BinaryMessage message) throws Exception {
        String username = (String) session.getAttributes().get("username");

        // Frame de tela vindo do agente → repassa direto para o cliente
        WebSocketSession clientSession = clientSessions.get(username);
        if (clientSession != null && clientSession.isOpen()) {
            clientSession.sendMessage(message);
        }
    }

    // ─── Conexão encerrada ───────────────────────────────────────────────────────

    @Override
    public void afterConnectionClosed(WebSocketSession session,
                                      CloseStatus status) throws Exception {
        String username = (String) session.getAttributes().get("username");
        String role     = getRole(session);

        if ("agent".equals(role)) {
            agentSessions.remove(username);
            notifyClient(username, CommandType.AGENT_DISCONNECTED, "Agente desconectado");
        } else {
            clientSessions.remove(username);
            notifyAgent(username, CommandType.CLIENT_DISCONNECTED, "Cliente desconectado");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private void forwardToAgent(String username, RelayMessage message) throws IOException {
        WebSocketSession agentSession = agentSessions.get(username);
        if (agentSession != null && agentSession.isOpen()) {
            sendText(agentSession, message);
        }
    }

    private void forwardToClient(String username, RelayMessage message) throws IOException {
        WebSocketSession clientSession = clientSessions.get(username);
        if (clientSession != null && clientSession.isOpen()) {
            sendText(clientSession, message);
        }
    }

    private void notifyClient(String username, CommandType type, String payload) {
        WebSocketSession clientSession = clientSessions.get(username);
        if (clientSession != null && clientSession.isOpen()) {
            try {
                sendText(clientSession, RelayMessage.builder()
                        .type(type)
                        .sender("relay")
                        .payload(payload)
                        .timestamp(System.currentTimeMillis())
                        .build());
            } catch (IOException ignored) {}
        }
    }

    private void notifyAgent(String username, CommandType type, String payload) {
        WebSocketSession agentSession = agentSessions.get(username);
        if (agentSession != null && agentSession.isOpen()) {
            try {
                sendText(agentSession, RelayMessage.builder()
                        .type(type)
                        .sender("relay")
                        .payload(payload)
                        .timestamp(System.currentTimeMillis())
                        .build());
            } catch (IOException ignored) {}
        }
    }

    private void sendText(WebSocketSession session, RelayMessage message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(json));
    }

    private String getRole(WebSocketSession session) {
        // O papel vem como query param: /relay?token=xxx&role=agent
        String query = session.getUri() != null ? session.getUri().getQuery() : "";
        if (query != null && query.contains("role=agent")) return "agent";
        return "client";
    }
}