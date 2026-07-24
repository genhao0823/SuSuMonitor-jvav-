package com.susumonitor.server.module.server.controller;

import com.susumonitor.server.common.ApiResponse;
import com.susumonitor.server.module.server.service.AgentTokenService;
import com.susumonitor.server.module.server.vo.AgentTokenVo;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

/**
 * 提供管理员使用的 Agent Token 注册、轮换和撤销接口。
 */
@RestController
@RequestMapping("/api/servers")
@Validated
@RequiredArgsConstructor
public class AgentTokenController {

    private final AgentTokenService agentTokenService;

    /** 首次生成 Agent Token。 */
    @PostMapping("/{id}/agent/register")
    public ApiResponse<AgentTokenVo> register(@PathVariable("id") @Positive Long serverId) {
        return ApiResponse.success(agentTokenService.register(serverId));
    }

    /** 显式轮换已有 Agent Token。 */
    @PostMapping("/{id}/agent/rotate")
    public ApiResponse<AgentTokenVo> rotate(@PathVariable("id") @Positive Long serverId) {
        return ApiResponse.success(agentTokenService.rotate(serverId));
    }

    /** 撤销当前 Agent Token。 */
    @DeleteMapping("/{id}/agent/revoke")
    public ApiResponse<Void> revoke(@PathVariable("id") @Positive Long serverId) {
        agentTokenService.revoke(serverId);
        return ApiResponse.success(null);
    }
}
