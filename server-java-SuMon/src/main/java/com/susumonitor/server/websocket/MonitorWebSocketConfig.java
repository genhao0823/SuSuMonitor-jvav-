package com.susumonitor.server.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** 注册浏览器监控 WebSocket 通道。 */
@Configuration
public class MonitorWebSocketConfig implements WebSocketConfigurer {

    private final MonitorWebSocketHandler monitorWebSocketHandler;
    private final MonitorTicketService monitorTicketService;

    /** 注入浏览器监控 Handler。 */
    public MonitorWebSocketConfig(MonitorWebSocketHandler monitorWebSocketHandler,
            MonitorTicketService monitorTicketService) {
        this.monitorWebSocketHandler = monitorWebSocketHandler;
        this.monitorTicketService = monitorTicketService;
    }

    /** 注册 /ws/monitor 地址。 */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(monitorWebSocketHandler, "/ws/monitor")
                .addInterceptors(new MonitorHandshakeInterceptor(monitorTicketService))
                .setAllowedOriginPatterns("*");
    }
}
