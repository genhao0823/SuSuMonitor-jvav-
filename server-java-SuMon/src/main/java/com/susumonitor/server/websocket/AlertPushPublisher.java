package com.susumonitor.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.susumonitor.server.module.alert.service.AlertTriggeredEvent;
import com.susumonitor.server.module.alert.vo.AlertRecordVo;
import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.TextMessage;

/**
 * 在告警评估事务提交后，向订阅了该服务器的 Monitor 会话推送 alert.push。
 *
 * <p>复用 MonitorSubscriptionRegistry，不需要新建 WebSocket 通道。
 * 推送帧 type=alert.push，payload 包含 server_id 和告警记录 VO。
 * 推送失败时移除订阅会话，不影响其他订阅者。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertPushPublisher {

    private final ObjectMapper objectMapper;
    private final MonitorSubscriptionRegistry registry;
    private final Clock clock;

    /**
     * 消费 AlertTriggeredEvent，在告警事务提交后推送。
     *
     * <p>AFTER_COMMIT 确保告警记录和状态已落库后才推送。
     * 如果推送失败，移除订阅会话但不抛异常，避免影响其他订阅者。</p>
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAlertTriggered(AlertTriggeredEvent event) {
        for (MonitorWebSocketSession subscriber : registry.subscribers(event.serverId())) {
            try {
                if (subscriber.socketSession().isOpen()) {
                    subscriber.socketSession().sendMessage(new TextMessage(message(event)));
                }
            } catch (IOException exception) {
                registry.remove(subscriber);
            }
        }
    }

    /** 构建 alert.push WebSocket 消息帧。 */
    private String message(AlertTriggeredEvent event) throws IOException {
        AlertRecordVo record = event.record();
        var alertPayload = objectMapper.createObjectNode()
                .put("server_id", event.serverId())
                .set("alert", objectMapper.valueToTree(record));
        return objectMapper.createObjectNode()
                .put("type", "alert.push")
                .put("message_id", UUID.randomUUID().toString())
                .put("timestamp", OffsetDateTime.now(clock).toString())
                .set("payload", alertPayload)
                .toString();
    }
}
