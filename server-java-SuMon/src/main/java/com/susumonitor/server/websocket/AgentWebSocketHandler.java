package com.susumonitor.server.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.susumonitor.server.common.BusinessException;
import com.susumonitor.server.module.server.entity.ServerEntity;
import com.susumonitor.server.module.metrics.dto.MetricReportMessage;
import com.susumonitor.server.module.metrics.service.MetricsService;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 处理 Agent 文本 WebSocket 首帧鉴权和心跳消息，不接收浏览器监控订阅。
 */
@Slf4j
@Component
public class AgentWebSocketHandler extends TextWebSocketHandler {

    private static final int MAX_MESSAGE_BYTES = 64 * 1024;
    private final ObjectMapper objectMapper;
    private final AgentAuthenticationService authenticationService;
    private final AgentHeartbeatService heartbeatService;
    private final AgentConnectionRegistry connectionRegistry;
    private final MetricsService metricsService;
    private final Clock clock;
    private final Map<String, AgentWebSocketSession> pendingSessions = new ConcurrentHashMap<>();

    /** 注入 JSON、鉴权、心跳和连接注册依赖。 */
    public AgentWebSocketHandler(
            ObjectMapper objectMapper,
            AgentAuthenticationService authenticationService,
            AgentHeartbeatService heartbeatService,
            AgentConnectionRegistry connectionRegistry,
            MetricsService metricsService,
            Clock clock) {
        this.objectMapper = objectMapper;
        this.authenticationService = authenticationService;
        this.heartbeatService = heartbeatService;
        this.connectionRegistry = connectionRegistry;
        this.metricsService = metricsService;
        this.clock = clock;
    }

    /** 保存新连接，等待其发送首帧认证。 */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        pendingSessions.put(session.getId(), new AgentWebSocketSession(session, clock));
    }

    /** 解析并分发 Agent 消息。 */
    @Override
    protected void handleTextMessage(WebSocketSession socketSession, TextMessage message) throws IOException {
        if (message.asBytes().length > MAX_MESSAGE_BYTES) {
            close(socketSession, new CloseStatus(1009, "message too big"));
            return;
        }
        AgentWebSocketSession session = pendingSessions.get(socketSession.getId());
        if (session == null) {
            close(socketSession, CloseStatus.SESSION_NOT_RELIABLE);
            return;
        }
        try {
            AgentMessage agentMessage = objectMapper.readValue(message.getPayload(), AgentMessage.class);
            if (AgentMessageType.AGENT_AUTHENTICATE.value().equals(agentMessage.type())) {
                authenticate(session, agentMessage.payload());
            } else if (AgentMessageType.HEARTBEAT.value().equals(agentMessage.type()) && session.authenticated()) {
                heartbeatService.heartbeat(session);
                send(session.socketSession(), AgentMessageType.HEARTBEAT_ACK, agentMessage.messageId(),
                        objectMapper.createObjectNode());
            } else if ("metrics.report".equals(agentMessage.type()) && session.authenticated()) {
                MetricReportMessage report = objectMapper.treeToValue(
                        objectMapper.valueToTree(agentMessage), MetricReportMessage.class);
                metricsService.report(session.serverId(), report.getPayload());
            } else {
                sendError(session.socketSession(), agentMessage.messageId(), "invalid agent message state");
            }
        } catch (BusinessException exception) {
            sendError(socketSession, null, exception.getErrorCode().getMessage());
            if (!session.authenticated()) {
                close(socketSession, CloseStatus.POLICY_VIOLATION);
            }
        } catch (IllegalStateException exception) {
            // 心跳目标不可用等运行时状态异常单独处理，不误报为消息格式错误，也不关闭已认证连接。
            log.warn("agent runtime state error, sessionId={}, message={}", socketSession.getId(),
                    exception.getMessage());
            sendError(socketSession, null, "agent runtime state error");
        } catch (Exception exception) {
            sendError(socketSession, null, "invalid agent message");
            close(socketSession, CloseStatus.BAD_DATA);
        }
    }

    /** 连接关闭时清理待认证或已认证会话。 */
    @Override
    public void afterConnectionClosed(WebSocketSession socketSession, CloseStatus status) {
        AgentWebSocketSession session = pendingSessions.remove(socketSession.getId());
        if (session != null && session.authenticated()) {
            connectionRegistry.remove(session);
        }
    }

    private void authenticate(AgentWebSocketSession session, JsonNode payload) throws IOException {
        if (session.authenticated() || payload == null
                || !payload.hasNonNull("server_id") || !payload.hasNonNull("token")
                || !payload.get("token").isTextual() || payload.get("token").textValue().isBlank()) {
            throw new BusinessException(com.susumonitor.server.common.ErrorCode.UNAUTHORIZED);
        }
        // 标记认证中，避免超时扫描在 DB 认证未完成时关闭连接。
        session.markAuthenticating();
        Long serverId = payload.get("server_id").longValue();
        ServerEntity server = authenticationService.authenticate(serverId, payload.get("token").textValue());
        session.authenticate(server.getId(), LocalDateTime.now(clock));
        heartbeatService.heartbeat(session);
        // 注册前校验连接仍在，避免把已关闭会话注册进 registry。
        if (session.socketSession().isOpen()) {
            connectionRegistry.replace(session).ifPresent(old -> close(old.socketSession(), CloseStatus.NORMAL));
            send(session.socketSession(), AgentMessageType.AGENT_AUTHENTICATED, null, objectMapper.createObjectNode());
        }
    }

    /** 清理十秒内未发送认证首帧的连接，跳过认证中状态避免误杀慢 DB 认证。 */
    @Scheduled(fixedDelay = 5_000)
    public void closeUnauthenticatedSessions() {
        Instant cutoff = Instant.now(clock).minus(Duration.ofSeconds(10));
        pendingSessions.values().removeIf(session -> {
            if (session.authenticated() || session.authenticating() || session.connectedAt().isAfter(cutoff)) {
                return false;
            }
            close(session.socketSession(), CloseStatus.POLICY_VIOLATION);
            return true;
        });
    }

    private void send(WebSocketSession session, AgentMessageType type, String messageId, JsonNode payload)
            throws IOException {
        if (session.isOpen()) {
            String body = objectMapper.createObjectNode()
                    .put("type", type.value())
                    .put("message_id", messageId)
                    .put("timestamp", OffsetDateTime.now(clock).toString())
                    .set("payload", payload).toString();
            session.sendMessage(new TextMessage(body));
        }
    }

    private void sendError(WebSocketSession session, String messageId, String message) throws IOException {
        send(session, AgentMessageType.ERROR, messageId,
                objectMapper.createObjectNode().put("message", message));
    }

    private void close(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException ignored) {
            log.debug("Agent WebSocket close failed, sessionId={}", session.getId());
        }
    }
}
