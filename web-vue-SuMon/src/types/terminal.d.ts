/**
 * Web 终端协议类型(MVP-7 T4 前端)。
 *
 * 字段命名与协议文档 `docs-SuMon/Protocol-SuMon/websocket-protocol.md §Terminal Messages`
 * 严格对齐(snake_case 与后端 Java / Go Agent 保持一致)。
 *
 * 浏览器 -> Java(/ws/monitor)的 4 种 control frame:
 *   - terminal.open    payload: server_id, cols (1-300), rows (1-100)
 *   - terminal.input   payload: session_id, data (Base64, decoded 1-16 KiB)
 *   - terminal.resize  payload: session_id, cols (1-300), rows (1-100)
 *   - terminal.close   payload: session_id
 *
 * Java -> 浏览器的 4 种响应 frame(Java 注入 server_id / session_id):
 *   - terminal.opened  payload: server_id, session_id, shell
 *   - terminal.output  payload: server_id, session_id, data (Base64, decoded 1-16 KiB)
 *   - terminal.closed  payload: server_id, session_id, reason (1-128 chars)
 *   - terminal.error   payload: server_id, optional session_id, code, message (1-256 chars)
 *
 * 浏览器必须只发前 4 种,后端会拒绝其他方向;本文件同时给出全部 8 种,
 * 仅为类型完整与文档目的(便于测试与 Mock)。
 */

/** 通用 WS 帧外壳(仅声明客户端关心的字段)。 */
export interface TerminalFrame<T extends string, P> {
  type: T
  message_id: string
  timestamp: string
  payload: P
}

/* -------------------- 浏览器 -> Java /ws/monitor -------------------- */

/** terminal.open payload: 申请在指定 serverId 上开启 root PTY 会话。 */
export interface TerminalOpenPayload {
  server_id: number
  cols: number
  rows: number
}
export type TerminalOpenFrame = TerminalFrame<'terminal.open', TerminalOpenPayload>

/** terminal.input payload: 往指定 sessionId 发送键盘输入(Base64 编码的 UTF-8)。 */
export interface TerminalInputPayload {
  session_id: string
  data: string
}
export type TerminalInputFrame = TerminalFrame<'terminal.input', TerminalInputPayload>

/** terminal.resize payload: 调整指定 sessionId 的终端尺寸。 */
export interface TerminalResizePayload {
  session_id: string
  cols: number
  rows: number
}
export type TerminalResizeFrame = TerminalFrame<'terminal.resize', TerminalResizePayload>

/** terminal.close payload: 主动关闭指定 sessionId。 */
export interface TerminalClosePayload {
  session_id: string
}
export type TerminalCloseFrame = TerminalFrame<'terminal.close', TerminalClosePayload>

/* -------------------- Java -> 浏览器 -------------------- */

/** terminal.opened payload: 后端确认 PTY 已打开,告知浏览器 sessionId。 */
export interface TerminalOpenedPayload {
  server_id: number
  session_id: string
  shell: string
}
export type TerminalOpenedFrame = TerminalFrame<'terminal.opened', TerminalOpenedPayload>

/** terminal.output payload: PTY 输出(Base64 编码的 UTF-8)。 */
export interface TerminalOutputPayload {
  server_id: number
  session_id: string
  data: string
}
export type TerminalOutputFrame = TerminalFrame<'terminal.output', TerminalOutputPayload>

/** terminal.closed payload: PTY 因任何原因已关闭,reason 1-128 字符。 */
export interface TerminalClosedPayload {
  server_id: number
  session_id: string
  reason: string
}
export type TerminalClosedFrame = TerminalFrame<'terminal.closed', TerminalClosedPayload>

/** terminal.error payload: 协议层错误,code 复用 ErrorCode 常量。 */
export interface TerminalErrorPayload {
  server_id: number
  session_id?: string
  code: number
  message: string
}
export type TerminalErrorFrame = TerminalFrame<'terminal.error', TerminalErrorPayload>

/* -------------------- 终端专用错误码 -------------------- */

/**
 * Web 终端专用错误码,与协议文档 §Terminal Messages 一致。
 * 复用 src/types/error-code.ts 中的通用业务码作为基础。
 */
export const TerminalErrorCode = {
  /** 无效 payload(尺寸越界 / Base64 解码失败 / 字段缺失) */
  INVALID_PAYLOAD: 40003,
  /** 访问被拒绝(用户 review_status != approved / 非 Linux 主机族) */
  ACCESS_DENIED: 40302,
  /** session_id 不存在或已关闭 */
  SESSION_NOT_FOUND: 40403,
  /** session 状态冲突(对已关闭的 session 再次 close / resize) */
  SESSION_STATE_CONFLICT: 40903,
  /** Agent 离线 */
  AGENT_OFFLINE: 40904,
  /** session 数量上限 */
  SESSION_LIMIT_REACHED: 42903,
  /** 终端消息频率超限 */
  MESSAGE_LIMIT_REACHED: 42904
} as const

export type TerminalErrorCodeValue = (typeof TerminalErrorCode)[keyof typeof TerminalErrorCode]