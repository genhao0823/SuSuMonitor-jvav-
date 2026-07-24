package com.susumonitor.server.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.susumonitor.server.module.metrics.service.MetricsService;
import com.susumonitor.server.module.server.entity.ServerEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

/** 验证 Agent 首帧十秒超时边界和认证中会话保护。 */
class AgentWebSocketHandlerTests {

    private static final Instant CONNECTED_AT = Instant.parse("2026-07-22T00:00:00Z");

    /** 验证连接建立九秒时保留，十一秒时关闭。 */
    @Test
    void shouldCloseOnlyAfterAuthenticationTimeout() throws Exception {
        MutableClock clock = new MutableClock(CONNECTED_AT);
        WebSocketSession socket = socket("agent-timeout");
        AgentWebSocketHandler handler = handler(clock);
        handler.afterConnectionEstablished(socket);

        clock.set(CONNECTED_AT.plusSeconds(9));
        handler.closeUnauthenticatedSessions();
        verify(socket, never()).close(any(CloseStatus.class));

        clock.set(CONNECTED_AT.plusSeconds(11));
        handler.closeUnauthenticatedSessions();
        verify(socket).close(CloseStatus.POLICY_VIOLATION);
    }

    /** 验证认证中的慢连接超过十秒仍不会被扫描误杀。 */
    @Test
    void shouldNotCloseAuthenticatingSession() throws Exception {
        MutableClock clock = new MutableClock(CONNECTED_AT);
        WebSocketSession socket = socket("agent-authenticating");
        AgentAuthenticationService authenticationService = mock(AgentAuthenticationService.class);
        AgentHeartbeatService heartbeatService = mock(AgentHeartbeatService.class);
        CountDownLatch authenticationStarted = new CountDownLatch(1);
        CountDownLatch releaseAuthentication = new CountDownLatch(1);
        ServerEntity server = new ServerEntity();
        server.setId(1001L);
        when(authenticationService.authenticate(1001L, "test-token")).thenAnswer(invocation -> {
            authenticationStarted.countDown();
            if (!releaseAuthentication.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("authentication test timed out");
            }
            return server;
        });
        AgentWebSocketHandler handler = new AgentWebSocketHandler(new ObjectMapper(), authenticationService,
                heartbeatService, mock(AgentConnectionRegistry.class), mock(MetricsService.class), clock);
        handler.afterConnectionEstablished(socket);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> authentication = executor.submit(() -> {
                try {
                    handler.handleTextMessage(socket, new org.springframework.web.socket.TextMessage("""
                            {"type":"agent.authenticate","payload":{"server_id":1001,"token":"test-token"}}
                            """));
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            });
            org.junit.jupiter.api.Assertions.assertTrue(authenticationStarted.await(5, TimeUnit.SECONDS));

            clock.set(CONNECTED_AT.plusSeconds(11));
            handler.closeUnauthenticatedSessions();
            verify(socket, never()).close(any(CloseStatus.class));

            releaseAuthentication.countDown();
            authentication.get(5, TimeUnit.SECONDS);
        } finally {
            releaseAuthentication.countDown();
            executor.shutdownNow();
        }
    }

    private AgentWebSocketHandler handler(Clock clock) {
        return new AgentWebSocketHandler(new ObjectMapper(), mock(AgentAuthenticationService.class),
                mock(AgentHeartbeatService.class), mock(AgentConnectionRegistry.class),
                mock(MetricsService.class), clock);
    }

    private WebSocketSession socket(String id) {
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn(id);
        when(socket.isOpen()).thenReturn(true);
        return socket;
    }

    /** 提供测试可推进的 UTC Clock。 */
    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
