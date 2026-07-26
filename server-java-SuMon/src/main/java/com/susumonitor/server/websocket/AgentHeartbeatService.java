package com.susumonitor.server.websocket;

/**
 * 定义 Agent 心跳更新和过期会话扫描的运行时契约。
 */
public interface AgentHeartbeatService {

    /** 处理已认证 Agent 心跳。 */
    void heartbeat(AgentWebSocketSession session);

    /** 扫描过期会话并标记对应服务器离线。 */
    void markExpiredSessionsOffline();
}
