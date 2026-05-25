package com.deskpilot.relay.relay.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelayMessage {

    private CommandType type;
    private String      sessionId;
    private String      sender;     // "agent" ou "client"
    private String      payload;    // JSON com dados do comando ou frame base64
    private long        timestamp;
}