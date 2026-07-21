package com.susumonitor.server.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/** 返回一次性 Monitor WebSocket ticket。 */
public record MonitorTicketVo(
        @JsonProperty("ticket") String ticket,
        @JsonProperty("expires_at") OffsetDateTime expiresAt) {
}
