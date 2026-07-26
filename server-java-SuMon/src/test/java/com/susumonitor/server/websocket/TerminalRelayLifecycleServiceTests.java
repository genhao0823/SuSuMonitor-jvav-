package com.susumonitor.server.websocket;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.susumonitor.server.module.terminal.enums.TerminalSessionStatus;
import com.susumonitor.server.module.terminal.service.TerminalSessionService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/** 验证浏览器和 Agent 断开时的终端会话收口。 */
class TerminalRelayLifecycleServiceTests {
    /** 浏览器断开应通知 Agent 关闭 PTY，并移除会话路由和持久化会话。 */
    @Test
    void closeMonitorSessionsShouldSendCloseAndPersistClosure() throws Exception {
        TerminalSessionService sessions = mock(TerminalSessionService.class);
        AgentConnectionRegistry agents = mock(AgentConnectionRegistry.class);
        TerminalOutputRateLimiter limiter = mock(TerminalOutputRateLimiter.class);
        TerminalRelayRegistry registry = new TerminalRelayRegistry();
        MonitorWebSocketSession monitor = monitor("monitor-1");
        registry.bind("c4c942a2-8584-433f-8397-6aef8ea79391", 9L, monitor);
        when(agents.sendToServer(eq(9L), any(TextMessage.class))).thenReturn(true);
        TerminalRelayLifecycleService service = service(sessions, registry, agents, limiter);

        service.closeMonitorSessions(monitor);

        verify(agents).sendToServer(eq(9L), any(TextMessage.class));
        verify(sessions).closeSession("c4c942a2-8584-433f-8397-6aef8ea79391",
                TerminalSessionStatus.CLOSED.value(), "monitor_disconnected");
        verify(limiter).release("c4c942a2-8584-433f-8397-6aef8ea79391");
        org.junit.jupiter.api.Assertions.assertNull(registry.get("c4c942a2-8584-433f-8397-6aef8ea79391"));
    }

    /** 已被新连接替换的旧 Agent 断开时不得关闭仍由新连接服务的会话。 */
    @Test
    void closeAgentSessionsShouldIgnoreReplacedConnection() {
        TerminalSessionService sessions = mock(TerminalSessionService.class);
        AgentConnectionRegistry agents = mock(AgentConnectionRegistry.class);
        TerminalOutputRateLimiter limiter = mock(TerminalOutputRateLimiter.class);
        TerminalRelayRegistry registry = new TerminalRelayRegistry();
        registry.bind("c4c942a2-8584-433f-8397-6aef8ea79391", 9L, monitor("monitor-1"));
        AgentWebSocketSession oldAgent = agent(9L, "agent-old");
        when(agents.isCurrent(oldAgent)).thenReturn(false);

        service(sessions, registry, agents, limiter).closeAgentSessions(oldAgent);

        assertNotNull(registry.get("c4c942a2-8584-433f-8397-6aef8ea79391"));
        org.mockito.Mockito.verifyNoInteractions(sessions);
    }

    /** 当前 Agent 断开应收口关联会话并释放输出限流桶。 */
    @Test
    void closeAgentSessionsShouldReleaseOutputBucket() {
        TerminalSessionService sessions = mock(TerminalSessionService.class);
        AgentConnectionRegistry agents = mock(AgentConnectionRegistry.class);
        TerminalOutputRateLimiter limiter = mock(TerminalOutputRateLimiter.class);
        TerminalRelayRegistry registry = new TerminalRelayRegistry();
        registry.bind("c4c942a2-8584-433f-8397-6aef8ea79391", 9L, monitor("monitor-1"));
        AgentWebSocketSession currentAgent = agent(9L, "agent-current");
        when(agents.isCurrent(currentAgent)).thenReturn(true);

        service(sessions, registry, agents, limiter).closeAgentSessions(currentAgent);

        verify(sessions).closeSession("c4c942a2-8584-433f-8397-6aef8ea79391",
                TerminalSessionStatus.ERROR.value(), "agent_disconnected");
        verify(limiter).release("c4c942a2-8584-433f-8397-6aef8ea79391");
    }

    /** 服务端保护性关闭应通知 Agent，并收口持久化状态、路由绑定和输出桶。 */
    @Test
    void closeBindingFromServerShouldSendCloseAndReleaseAllSessionState() throws Exception {
        String sessionId = "c4c942a2-8584-433f-8397-6aef8ea79391";
        TerminalSessionService sessions = mock(TerminalSessionService.class);
        AgentConnectionRegistry agents = mock(AgentConnectionRegistry.class);
        TerminalOutputRateLimiter limiter = mock(TerminalOutputRateLimiter.class);
        TerminalRelayRegistry registry = new TerminalRelayRegistry();
        MonitorWebSocketSession monitor = monitor("monitor-1");
        registry.bind(sessionId, 9L, monitor);
        when(agents.sendToServer(eq(9L), any(TextMessage.class))).thenReturn(true);

        service(sessions, registry, agents, limiter).closeBindingFromServer(registry.get(sessionId),
                TerminalSessionStatus.CLOSED.value(), "output_rate_exceeded");

        verify(agents).sendToServer(eq(9L), any(TextMessage.class));
        verify(sessions).closeSession(sessionId, TerminalSessionStatus.CLOSED.value(), "output_rate_exceeded");
        verify(limiter).release(sessionId);
        org.junit.jupiter.api.Assertions.assertNull(registry.get(sessionId));
    }

    private TerminalRelayLifecycleService service(TerminalSessionService sessions, TerminalRelayRegistry registry,
            AgentConnectionRegistry agents, TerminalOutputRateLimiter limiter) {
        return new TerminalRelayLifecycleService(sessions, registry, agents, limiter, new ObjectMapper(), Clock.systemUTC());
    }

    private MonitorWebSocketSession monitor(String id) {
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn(id);
        return new MonitorWebSocketSession(socket, new com.susumonitor.server.security.AuthenticatedUser(
                1L, "user", "user", "approved", null, OffsetDateTime.now(ZoneOffset.UTC)));
    }

    private AgentWebSocketSession agent(Long serverId, String id) {
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn(id);
        AgentWebSocketSession agent = new AgentWebSocketSession(socket, Clock.systemUTC());
        agent.authenticate(serverId, java.time.LocalDateTime.now());
        return agent;
    }
}
