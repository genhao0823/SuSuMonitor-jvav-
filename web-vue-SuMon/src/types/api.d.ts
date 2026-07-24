/**
 * 项目级 API 类型定义。
 *
 * 字段命名规则:
 * - 认证模块(/api/auth/**)响应字段与 OpenAPI 一致,使用 camelCase。
 * - 服务器模块(/api/servers/**)响应字段与 OpenAPI 一致,使用 snake_case。
 * OpenAPI JSON 是唯一事实源,字段变更必须先改 JSON 再同步本文件。
 */

/**
 * 项目统一 API 响应包装。
 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/**
 * 统一分页结构(/api/servers 列表)。
 */
export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  page_size: number
}

/**
 * /api/health 响应数据。
 */
export interface HealthStatus {
  status: string
  application: string
  timestamp: string
}

/**
 * /api/ready 响应数据。
 */
export interface ReadyStatus {
  status: string
  database: string
  timestamp: string
}

/**
 * 用户角色枚举。
 */
export type UserRole = 'admin' | 'user'

/**
 * 用户审核状态枚举。
 */
export type ReviewStatus = 'pending' | 'approved' | 'rejected'

/**
 * 当前用户数据(与 OpenAPI CurrentUser schema 字段一致)。
 */
export interface CurrentUser {
  id: number
  username: string
  role: UserRole
  reviewStatus: ReviewStatus
  reviewedAt: string | null
  createdAt: string
}

/**
 * 登录结果(与 OpenAPI LoginResult schema 字段一致)。
 */
export interface LoginResult {
  token: string
  tokenType: string
  expiresIn: number
  user: CurrentUser
}

/**
 * 服务器状态枚举。
 */
export type ServerStatusKind = 'online' | 'offline' | 'unknown'

/**
 * Agent 状态枚举。
 */
export type AgentStatusKind = 'online' | 'offline'

/**
 * SSH 认证方式枚举。
 */
export type SshAuthType = 'password' | 'private_key'

/**
 * 服务器公开 VO,不包含凭据明文或密文。
 */
export interface Server {
  id: number
  name: string
  host: string
  description: string | null
  status: ServerStatusKind
  ssh_host: string
  ssh_port: number
  ssh_user: string
  ssh_auth_type: SshAuthType
  agent_id: string | null
  agent_status: AgentStatusKind
  last_heartbeat_at: string | null
  created_at: string
  updated_at: string
}

/**
 * 服务器状态快照。
 */
export interface ServerStatus {
  server_id: number
  status: ServerStatusKind
  agent_status: AgentStatusKind
  last_heartbeat_at: string | null
  checked_at: string
}

/**
 * 服务器列表查询参数。
 * 后端 OpenAPI 允许 keyword 单一字段模糊匹配;前端额外提供 name/host
 * 两个独立字段以满足"按 name 搜"与"按 host 搜"的精确场景。
 */
export interface ServerQuery {
  page?: number
  page_size?: number
  keyword?: string
  name?: string
  host?: string
  sort_by?: 'id' | 'name' | 'host' | 'created_at' | 'updated_at'
  sort_order?: 'asc' | 'desc'
}

/**
 * 创建服务器请求体,字段命名与 OpenAPI CreateServerRequest schema 对齐。
 * 凭据字段按 ssh_auth_type 二选一,空字符串或省略表示不修改/不设置。
 */
export interface CreateServerRequest {
  name: string
  host: string
  description?: string | null
  ssh_host: string
  ssh_port: number
  ssh_user: string
  ssh_auth_type: SshAuthType
  ssh_password?: string
  ssh_private_key?: string
  ssh_private_key_passphrase?: string
}

/**
 * 更新服务器请求体。所有字段可选;省略的字段后端保留原值。
 * 凭据字段在编辑模式下若保持空字符串则后端不更新该凭据(由调用方过滤)。
 */
export interface UpdateServerRequest {
  name?: string
  host?: string
  description?: string | null
  ssh_host?: string
  ssh_port?: number
  ssh_user?: string
  ssh_auth_type?: SshAuthType
  ssh_password?: string
  ssh_private_key?: string
  ssh_private_key_passphrase?: string
}

/**
 * SSH 连接测试结果(与后端 SshTestVo 字段对齐)。
 */
export interface SshTestResult {
  server_id: number
  connected: boolean
  host_key_algorithm: string | null
  host_key_fingerprint: string | null
  auth_type: string
  duration_ms: number
  tested_at: string
}