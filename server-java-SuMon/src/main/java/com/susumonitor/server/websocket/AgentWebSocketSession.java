package com.susumonitor.server.websocket;

import java.time.LocalDateTime;
import java.time.Instant;
import org.springframework.web.socket.WebSocketSession;

/**
 * 保存 Agent WebSocket 的认证归属和最近心跳，不保存明文 Token。
 */
public final class AgentWebSocketSession {

    private final WebSocketSession socketSession;
    private final String sessionId;
    private final Instant connectedAt;
    private Long serverId;
    private LocalDateTime lastHeartbeatAt;
    // 认证进行中标记：避免认证未完成时被未认证超时扫描误关闭。
    private volatile boolean authenticating;
    private boolean authenticated;

    /** 创建未认证的 WebSocket 会话包装。 */
    public AgentWebSocketSession(WebSocketSession socketSession) {
        this.socketSession = socketSession;
        this.sessionId = socketSession.getId();
        this.connectedAt = Instant.now();
    }

    public WebSocketSession socketSession() {
        return socketSession;
    }

    public String sessionId() {
        return sessionId;
    }

    public Instant connectedAt() {
        return connectedAt;
    }

    public Long serverId() {
        return serverId;
    }

    public void authenticate(Long serverId, LocalDateTime heartbeatAt) {
        this.serverId = serverId;
        this.lastHeartbeatAt = heartbeatAt;
        this.authenticated = true;
    }

    /** 标记会话进入认证中状态，防止超时扫描在认证未完成时关闭连接。 */
    public void markAuthenticating() {
        this.authenticating = true;
    }

    public boolean authenticating() {
        return authenticating;
    }

    public boolean authenticated() {
        return authenticated;
    }

    public LocalDateTime lastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void heartbeat(LocalDateTime heartbeatAt) {
        this.lastHeartbeatAt = heartbeatAt;
    }
}
