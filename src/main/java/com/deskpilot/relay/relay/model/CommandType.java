package com.deskpilot.relay.relay.model;

public enum CommandType {
    // Controle
    MOUSE_MOVE,
    MOUSE_CLICK,
    MOUSE_SCROLL,
    KEY_PRESS,
    KEY_RELEASE,
    TYPE_TEXT,

    // Stream
    SCREEN_FRAME,
    SCREEN_START,
    SCREEN_STOP,

    // Sessão
    AGENT_CONNECTED,
    AGENT_DISCONNECTED,
    CLIENT_CONNECTED,
    CLIENT_DISCONNECTED,
    PING,
    PONG
}