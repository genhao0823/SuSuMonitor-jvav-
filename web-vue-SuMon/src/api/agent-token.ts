import apiClient from '@/api/client'
import type { AgentToken, ApiResponse } from '@/types/api'

/**
 * Agent Token 模块 API 封装(MVP-2)。
 *
 * 三个端点对应 OpenAPI `openapi-server.json § /api/servers/{id}/agent/...`,
 * 均为 ADMIN 权限。后端只存 SHA-256 哈希,明文 Token 仅在 register / rotate
 * 成功响应中一次性返回,前端必须在 dialog 内展示并提示用户立即复制,
 * 不得写入 store / localStorage / 日志。
 *
 * 字段命名与 OpenAPI 一致(snake_case);鉴权失败由 `src/api/client.ts`
 * 统一拦截(40100 / 40300)。
 */

/**
 * 调用 POST /api/servers/{id}/agent/register 首次生成 Agent Token。
 *
 * 仅 ADMIN;返回的 `agent_token` 明文仅出现一次,前端必须立即复制。
 *
 * @param serverId 目标服务器 ID
 * @returns 包含一次性明文 Token 和创建时间的响应
 */
export function registerAgentToken(serverId: number): Promise<ApiResponse<AgentToken>> {
  return apiClient
    .post<ApiResponse<AgentToken>>(`/servers/${serverId}/agent/register`)
    .then((r) => r.data)
}

/**
 * 调用 POST /api/servers/{id}/agent/rotate 显式轮换 Agent Token。
 *
 * 仅 ADMIN;旧 Token 立即失效,新 Token 明文仅返回一次。
 *
 * @param serverId 目标服务器 ID
 * @returns 新 Token 与创建时间
 */
export function rotateAgentToken(serverId: number): Promise<ApiResponse<AgentToken>> {
  return apiClient
    .post<ApiResponse<AgentToken>>(`/servers/${serverId}/agent/rotate`)
    .then((r) => r.data)
}

/**
 * 调用 DELETE /api/servers/{id}/agent/revoke 撤销当前 Agent Token。
 *
 * 仅 ADMIN;成功后后端会把 agent_status 标记为 offline,Agent WS 断链。
 * 响应 data 为 null(EmptyResponse)。
 *
 * @param serverId 目标服务器 ID
 */
export function revokeAgentToken(serverId: number): Promise<ApiResponse<null>> {
  return apiClient
    .delete<ApiResponse<null>>(`/servers/${serverId}/agent/revoke`)
    .then((r) => r.data)
}