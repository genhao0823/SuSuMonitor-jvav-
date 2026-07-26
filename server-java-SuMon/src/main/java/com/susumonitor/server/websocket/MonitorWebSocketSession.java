package com.susumonitor.server.websocket;

import com.susumonitor.server.security.AuthenticatedUser;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import java.io.IOException;

/** 保存 Monitor WebSocket 的用户身份和服务器订阅集合。 */
public final class MonitorWebSocketSession {

    private final WebSocketSession socketSession;
    private final AuthenticatedUser user;
    private final Set<Long> subscribedServerIds = ConcurrentHashMap.newKeySet();

    /** 创建已通过握手 ticket 验证的 Monitor 会话。 */
    public MonitorWebSocketSession(WebSocketSession socketSession, AuthenticatedUser user) {
        this.socketSession = socketSession;
        this.user = user;
    }

    public WebSocketSession socketSession() {
        return socketSession;
    }

    public AuthenticatedUser user() {
        return user;
    }

    public Set<Long> subscribedServerIds() {
        return subscribedServerIds;
    }

    /** 串行化向单个浏览器连接的写入，避免终端输出与指标推送帧交错。 */
    public synchronized boolean send(TextMessage message) throws IOException {
        if (!socketSession.isOpen()) {
            return false;
        }
        socketSession.sendMessage(message);
        return true;
    }
}
