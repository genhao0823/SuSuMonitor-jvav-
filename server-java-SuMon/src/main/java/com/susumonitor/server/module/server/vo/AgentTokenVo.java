package com.susumonitor.server.module.server.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/**
 * Agent Token 一次性返回对象，明文 Token 仅在注册或轮换成功响应中出现。
 *
 * @param serverId 服务器 ID
 * @param agentToken 一次性显示的明文 Token
 * @param createdAt Token 创建或轮换时间
 */
public record AgentTokenVo(
        @JsonProperty("server_id") Long serverId,
        @JsonProperty("agent_token") String agentToken,
        @JsonProperty("created_at") OffsetDateTime createdAt) {
}
