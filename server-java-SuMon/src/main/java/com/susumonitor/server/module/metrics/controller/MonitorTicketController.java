package com.susumonitor.server.module.metrics.controller;

import com.susumonitor.server.common.ApiResponse;
import com.susumonitor.server.security.AuthenticatedUser;
import com.susumonitor.server.websocket.MonitorTicketService;
import com.susumonitor.server.websocket.MonitorTicketVo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 为已认证 Web 用户签发 Monitor WebSocket 一次性 ticket。 */
@RestController
@RequestMapping("/api/ws")
@RequiredArgsConstructor
public class MonitorTicketController {

    private final MonitorTicketService monitorTicketService;

    /** 签发 30 秒有效、只能使用一次的 Monitor ticket。 */
    @PostMapping("/monitor-ticket")
    public ApiResponse<MonitorTicketVo> issue(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(monitorTicketService.issue(user));
    }
}
