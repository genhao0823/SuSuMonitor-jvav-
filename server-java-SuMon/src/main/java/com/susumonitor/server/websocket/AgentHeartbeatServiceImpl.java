package com.susumonitor.server.websocket;

import com.susumonitor.server.module.server.mapper.ServerMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;

/**
 * 更新 Agent 心跳并扫描超过 90 秒未心跳的会话。
 */
@Service
public class AgentHeartbeatServiceImpl implements AgentHeartbeatService {

    private static final Duration HEARTBEAT_TIMEOUT = Duration.ofSeconds(90);
    private final ServerMapper serverMapper;
    private final AgentConnectionRegistry connectionRegistry;
    private final Clock clock;

    /** 注入服务器状态 Mapper 和 Agent 连接注册表。 */
    public AgentHeartbeatServiceImpl(ServerMapper serverMapper, AgentConnectionRegistry connectionRegistry,
            Clock clock) {
        this.serverMapper = serverMapper;
        this.connectionRegistry = connectionRegistry;
        this.clock = clock;
    }

    /** 处理已认证 Agent 心跳。 */
    public void heartbeat(AgentWebSocketSession session) {
        LocalDateTime heartbeatAt = LocalDateTime.now(clock);
        if (serverMapper.updateAgentHeartbeat(session.serverId(), heartbeatAt) != 1) {
            throw new IllegalStateException("Agent heartbeat target is unavailable");
        }
        session.heartbeat(heartbeatAt);
    }

    /** 每 30 秒扫描过期会话并标记服务器离线。 */
    @Scheduled(fixedDelay = 30_000)
    public void markExpiredSessionsOffline() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minus(HEARTBEAT_TIMEOUT);
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

    /** Agent 连接断开时标记服务器离线(乐观锁,不覆盖已重连新连接)。 */
    @Override
    public void markOfflineOnDisconnect(AgentWebSocketSession session) {
        if (session == null || !session.authenticated() || session.serverId() == null
                || session.lastHeartbeatAt() == null) {
            return;
        }
        // 乐观锁:仅当 last_heartbeat_at 仍是断开时的值才设 offline,
        // 防止误把已重连新连接(新心跳更新了 last_heartbeat_at)设为离线。
        serverMapper.markAgentOffline(session.serverId(), session.lastHeartbeatAt());
    }
}
