package com.susumonitor.server.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.susumonitor.server.module.server.mapper.ServerMapper;
import com.susumonitor.server.security.AuthenticatedUser;
import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 处理已通过一次性 ticket 握手的浏览器指标订阅。
 *
 * <p>当前授权策略：admin 或 review_status=approved 的已认证用户均可订阅任意存在的服务器指标。
 * 上游 JWT 过滤器强制要求所有通过认证的用户 review_status=approved，故此处 approved 判断恒为真，
 * 实际起作用的是 role=admin 的分支；该判断保留以明确授权意图，便于后续按服务器粒度收紧。</p>
 */
@Component
public class MonitorWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ServerMapper serverMapper;
    private final MonitorSubscriptionRegistry registry;
    private final Clock clock;
    private final Map<String, MonitorWebSocketSession> sessions = new ConcurrentHashMap<>();

    /** 注入 JSON、服务器权限查询和订阅注册表。 */
    public MonitorWebSocketHandler(ObjectMapper objectMapper, ServerMapper serverMapper,
            MonitorSubscriptionRegistry registry, Clock clock) {
        this.objectMapper = objectMapper;
        this.serverMapper = serverMapper;
        this.registry = registry;
        this.clock = clock;
    }

    /** 从握手属性取得用户身份并注册连接。 */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        Object principal = session.getAttributes().get("authenticated_user");
        if (!(principal instanceof AuthenticatedUser user)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        MonitorWebSocketSession monitorSession = new MonitorWebSocketSession(session, user);
        sessions.put(session.getId(), monitorSession);
        registry.register(monitorSession);
    }

    /** 处理 metrics.subscribe 和 metrics.unsubscribe 消息，坏 JSON 回送错误帧而非断连。 */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        MonitorWebSocketSession monitorSession = sessions.get(session.getId());
        if (monitorSession == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        JsonNode body;
        try {
            body = objectMapper.readTree(message.getPayload());
        } catch (JsonProcessingException exception) {
            // 一条格式错误的 JSON 不应让整个 Monitor 连接断开，回送错误帧后保留连接。
            sendError(session, "invalid json");
            return;
        }
        String type = body == null || !body.has("type") || !body.get("type").isTextual()
                ? "" : body.get("type").textValue();
        // path 在缺失时返回 MissingNode 而非 null，用 isMissingNode 表意更清晰。
        JsonNode serverNode = body == null ? null : body.path("payload").path("server_id");
        if (serverNode == null || serverNode.isMissingNode()
                || !serverNode.canConvertToLong() || serverNode.longValue() <= 0) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        Long serverId = serverNode.longValue();
        if (serverMapper.selectActiveServerById(serverId) == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        // 当前授权策略：admin 或 approved 用户均可订阅，详见类注释。
        if (!"admin".equals(monitorSession.user().role())
                && !"approved".equals(monitorSession.user().reviewStatus())) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        if ("metrics.subscribe".equals(type)) {
            registry.subscribe(monitorSession, serverId);
        } else if ("metrics.unsubscribe".equals(type)) {
            registry.unsubscribe(monitorSession, serverId);
        } else {
            session.close(CloseStatus.BAD_DATA);
        }
    }

    /** 清理连接及其全部服务器订阅。 */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        MonitorWebSocketSession monitorSession = sessions.remove(session.getId());
        if (monitorSession != null) {
            registry.remove(monitorSession);
        }
    }

    /** 向 Monitor 连接回送错误帧，不关闭连接。 */
    private void sendError(WebSocketSession session, String message) throws IOException {
        if (session.isOpen()) {
            String body = objectMapper.createObjectNode()
                    .put("type", "error")
                    .put("message_id", java.util.UUID.randomUUID().toString())
                    .put("timestamp", OffsetDateTime.now(clock).toString())
                    .set("payload", objectMapper.createObjectNode().put("message", message))
                    .toString();
            session.sendMessage(new TextMessage(body));
        }
    }
}
