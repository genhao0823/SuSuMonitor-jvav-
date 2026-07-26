package com.susumonitor.server.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.susumonitor.server.common.BusinessException;
import com.susumonitor.server.common.ErrorCode;
import com.susumonitor.server.module.terminal.service.TerminalSessionService;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/** 验证 Agent 终端响应按服务器和会话归属回送原浏览器。 */
class TerminalAgentRelayServiceTests {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SESSION_ID = "c4c942a2-8584-433f-8397-6aef8ea79391";

    /** Agent 的 terminal.opened 应更新元数据并回送原浏览器。 */
    @Test
    void openedShouldUpdateSessionAndForwardToMonitor() throws Exception {
        TerminalSessionService sessions = mock(TerminalSessionService.class);
        TerminalRelayRegistry registry = new TerminalRelayRegistry();
        MonitorWebSocketSession monitor = monitor();
        registry.bind(SESSION_ID, 9L, monitor);
        AgentWebSocketSession agent = agent(9L);
        TerminalAgentRelayService service = new TerminalAgentRelayService(sessions, registry, OBJECT_MAPPER,
                Clock.fixed(Instant.parse("2026-07-26T00:00:00Z"), ZoneOffset.UTC));

        service.relay(agent, message("terminal.opened", "session_id", SESSION_ID, "shell", "bash"));

        verify(sessions).markOpened(SESSION_ID, "bash");
        verify(monitor.socketSession()).sendMessage(any(TextMessage.class));
    }

    /** 伪造其他服务器 ID 的 Agent 响应不得路由到浏览器。 */
    @Test
    void responseShouldRejectDifferentAgentServer() {
        TerminalRelayRegistry registry = new TerminalRelayRegistry();
        registry.bind(SESSION_ID, 9L, monitor());
        TerminalAgentRelayService service = new TerminalAgentRelayService(mock(TerminalSessionService.class), registry,
                OBJECT_MAPPER, Clock.systemUTC());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.relay(agent(8L), message("terminal.opened", "session_id", SESSION_ID, "shell", "bash")));

        assertEquals(ErrorCode.TERMINAL_ACCESS_DENIED, exception.getErrorCode());
    }

    private TerminalMessage message(String type, String sessionField, String sessionId, String valueField, String value) {
        var payload = OBJECT_MAPPER.createObjectNode().put("server_id", 9).put(sessionField, sessionId).put(valueField, value);
        return new TerminalMessage(type, UUID.randomUUID().toString(), OffsetDateTime.now(ZoneOffset.UTC).toString(), payload);
    }

    private MonitorWebSocketSession monitor() {
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.isOpen()).thenReturn(true);
        return new MonitorWebSocketSession(socket, new com.susumonitor.server.security.AuthenticatedUser(
                1L, "user", "user", "approved", null, OffsetDateTime.now(ZoneOffset.UTC)));
    }

    private AgentWebSocketSession agent(Long serverId) {
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn("agent-" + serverId);
        when(socket.isOpen()).thenReturn(true);
        AgentWebSocketSession session = new AgentWebSocketSession(socket, Clock.systemUTC());
        session.authenticate(serverId, java.time.LocalDateTime.now());
        return session;
    }
}
