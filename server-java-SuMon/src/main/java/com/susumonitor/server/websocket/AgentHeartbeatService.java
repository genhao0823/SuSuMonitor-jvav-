package com.susumonitor.server.websocket;

import com.susumonitor.server.module.server.mapper.ServerMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;

/**
 * 更新 Agent 心跳并扫描超过 90 秒未心跳的会话。
 */
@Service
public class AgentHeartbeatService {

    private static final Duration HEARTBEAT_TIMEOUT = Duration.ofSeconds(90);
    private final ServerMapper serverMapper;
    private final AgentConnectionRegistry connectionRegistry;

    /** 注入服务器状态 Mapper 和 Agent 连接注册表。 */
    public AgentHeartbeatService(ServerMapper serverMapper, AgentConnectionRegistry connectionRegistry) {
        this.serverMapper = serverMapper;
        this.connectionRegistry = connectionRegistry;
    }

    /** 处理已认证 Agent 心跳。 */
    public void heartbeat(AgentWebSocketSession session) {
        LocalDateTime heartbeatAt = LocalDateTime.now(ZoneOffset.UTC);
        if (serverMapper.updateAgentHeartbeat(session.serverId(), heartbeatAt) != 1) {
            throw new IllegalStateException("Agent heartbeat target is unavailable");
        }
        session.heartbeat(heartbeatAt);
    }

    /** 每 30 秒扫描过期会话并标记服务器离线。 */
    @Scheduled(fixedDelay = 30_000)
    public void markExpiredSessionsOffline() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minus(HEARTBEAT_TIMEOUT);
        for (AgentWebSocketSession session : connectionRegistry.sessions()) {
            if (session.authenticated() && session.lastHeartbeatAt() != null
                    && session.lastHeartbeatAt().isBefore(cutoff)) {
                serverMapper.markAgentOffline(session.serverId(), session.lastHeartbeatAt());
                connectionRegistry.remove(session);
                if (session.socketSession().isOpen()) {
                    try {
                        session.socketSession().close(CloseStatus.SESSION_NOT_RELIABLE);
                    } catch (Exception ignored) {
                        // 扫描任务继续处理其他会话。
                    }
                }
            }
        }
    }
}
