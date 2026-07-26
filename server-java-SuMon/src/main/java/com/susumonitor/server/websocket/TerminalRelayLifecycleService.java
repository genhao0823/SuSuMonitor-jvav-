package com.susumonitor.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.susumonitor.server.module.terminal.enums.TerminalSessionStatus;
import com.susumonitor.server.module.terminal.service.TerminalSessionService;
import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

/** 在 WebSocket 断开时收口终端元数据、内存路由和远端 PTY。 */
@Component
public class TerminalRelayLifecycleService {
    private final TerminalSessionService terminalSessionService;
    private final TerminalRelayRegistry relayRegistry;
    private final AgentConnectionRegistry agentRegistry;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public TerminalRelayLifecycleService(TerminalSessionService terminalSessionService, TerminalRelayRegistry relayRegistry,
            AgentConnectionRegistry agentRegistry, ObjectMapper objectMapper, Clock clock) {
        this.terminalSessionService = terminalSessionService;
        this.relayRegistry = relayRegistry;
        this.agentRegistry = agentRegistry;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /** 浏览器断开后通知在线 Agent 关闭 PTY，并收口对应会话元数据。 */
    public void closeMonitorSessions(MonitorWebSocketSession monitorSession) {
        for (TerminalRelayRegistry.TerminalRelayBinding binding : relayRegistry.removeByMonitorSession(monitorSession)) {
            sendClose(binding);
            terminalSessionService.closeSession(binding.sessionId(), TerminalSessionStatus.CLOSED.value(), "monitor_disconnected");
        }
    }

    /** 当前 Agent 断开后将其未完成会话标记为错误；被替换的旧连接不触发收口。 */
    public void closeAgentSessions(AgentWebSocketSession agentSession) {
        if (!agentRegistry.isCurrent(agentSession)) {
            return;
        }
        for (TerminalRelayRegistry.TerminalRelayBinding binding : relayRegistry.removeByServerId(agentSession.serverId())) {
            terminalSessionService.closeSession(binding.sessionId(), TerminalSessionStatus.ERROR.value(), "agent_disconnected");
        }
    }

    /** 断连清理使用服务端生成的关闭帧，失败后仍必须完成元数据收口。 */
    private void sendClose(TerminalRelayRegistry.TerminalRelayBinding binding) {
        String body = objectMapper.createObjectNode().put("type", TerminalMessageType.TERMINAL_CLOSE.value())
                .put("message_id", UUID.randomUUID().toString()).put("timestamp", OffsetDateTime.now(clock).toString())
                .set("payload", objectMapper.createObjectNode().put("server_id", binding.serverId())
                        .put("session_id", binding.sessionId())).toString();
        try {
            agentRegistry.sendToServer(binding.serverId(), new TextMessage(body));
        } catch (IOException ignored) {
            // Agent 已断开时仍应关闭元数据，避免遗留可操作会话。
        }
    }
}
