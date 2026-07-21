package com.susumonitor.server.websocket;

import java.time.OffsetDateTime;

/** 返回一次性 Monitor WebSocket ticket。 */
public record MonitorTicketVo(String ticket, OffsetDateTime expiresAt) {
}
