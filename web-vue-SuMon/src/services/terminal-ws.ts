import type {
  TerminalCloseFrame,
  TerminalOpenedFrame,
  TerminalOutputFrame,
  TerminalResizeFrame
} from '@/types/terminal'

/**
 * UTF-8 <-> Base64 编解码工具,封装浏览器原生 API,
 * 避免引入 Buffer / polyfill(协议层 1-16 KiB 用 UTF-8 安全传输即可)。
 */
function encodeBase64(input: string): string {
  const bytes = new TextEncoder().encode(input)
  let binary = ''
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i])
  }
  return btoa(binary)
}

function decodeBase64(base64: string): string {
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < bytes.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return new TextDecoder().decode(bytes)
}

/**
 * 生成 UUID v4 字符串,用于终端 WS 帧 message_id。
 *
 * 注意:后端 TerminalProtocolValidator 校验 message_id 必须为 UUID。
 * crypto.randomUUID 仅在安全上下文(HTTPS/localhost)可用;站点以明文 HTTP
 * 提供服务时该方法不存在,需降级到 crypto.getRandomValues(非安全上下文可用)
 * 手动拼装 UUID v4,否则 terminal.open 会因 message_id 非 UUID 被后端拒绝
 * (40003 invalid payload),表现为终端建链失败、黑框无响应。
 */
function newTerminalMessageId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return uuidv4Fallback()
}

/** 非安全上下文降级:用 getRandomValues 生成标准 UUID v4(RFC 4122)。 */
function uuidv4Fallback(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
    const b = new Uint8Array(16)
    crypto.getRandomValues(b)
    b[6] = (b[6] & 0x0f) | 0x40
    b[8] = (b[8] & 0x3f) | 0x80
    const h: string[] = []
    for (let i = 0; i < 16; i++) {
      h.push(b[i].toString(16).padStart(2, '0'))
    }
    return `${h.slice(0, 4).join('')}-${h.slice(4, 6).join('')}-${h.slice(6, 8).join('')}-${h.slice(8, 10).join('')}-${h.slice(10, 16).join('')}`
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

/** 把任意字符串压缩为 WS 帧外壳(自动生成 message_id + UTC ISO timestamp)。 */
function wrapFrame<T extends string, P>(
  type: T,
  payload: P
): { type: T; message_id: string; timestamp: string; payload: P } {
  return {
    type,
    message_id: newTerminalMessageId(),
    timestamp: new Date().toISOString(),
    payload
  }
}

/**
 * 终端连接生命周期阶段。
 * - `idle`: 未开始建链
 * - `connecting`: 已发 ticket,等待 socket.onopen
 * - `awaiting_open`: socket 已开,已发 terminal.open,等待 terminal.opened
 * - `open`: 收到 terminal.opened,后续 input/resize/close 已可用
 * - `closing`: 已发 terminal.close 或主动 close,等待 terminal.closed / socket close
 * - `closed`: socket 已 close
 */
export type TerminalPhase = 'idle' | 'connecting' | 'awaiting_open' | 'open' | 'closing' | 'closed'

/** 终端回调集合(由 TerminalView 注入,xterm 与状态条使用)。 */
export interface TerminalHandlers {
  /** 收到 terminal.opened,session_id 已知 */
  onOpened: (sessionId: string, shell: string) => void
  /** 收到 terminal.output,已 Base64 解码为 UTF-8 文本 */
  onOutput: (text: string) => void
  /** 收到 terminal.closed 或 socket 异常断开,reason 已规范化 */
  onClosed: (reason: string) => void
  /** 收到 terminal.error,UI 应展示并退出 */
  onError: (code: number, message: string) => void
}

/**
 * 终端 WS 客户端:复用 MonitorWebSocket 已建链的 WebSocket,通过 injectMessageListener
 * 注入回调拦截 terminal.* 帧;在视图层一个 serverId 一个 TerminalWebSocket 实例。
 *
 * 设计约束:
 * - 单一职责:只管终端会话生命周期,不维护 metrics / alert 状态
 * - 不自动重连:WebSocket 断开会话丢失;如需重建,UI 重新挂载组件
 * - 输入采用 UTF-8 Base64(协议层 1-16 KiB 上限)
 * - Base64 工具用浏览器原生 API + TextEncoder/TextDecoder
 */
export class TerminalWebSocket {
  private phase: TerminalPhase = 'idle'
  private sessionId: string | null = null
  private readonly serverId: number
  private readonly cols: number
  private readonly rows: number
  private readonly handlers: TerminalHandlers

  /**
   * 由调用方(典型为 TerminalView)提供的 WebSocket 句柄,
   * 调用方确保它处于 OPEN 状态(MonitorWebSocket.onopen 已发 metrics.subscribe)。
   */
  private readonly socket: WebSocket

  constructor(params: {
    socket: WebSocket
    serverId: number
    cols: number
    rows: number
    handlers: TerminalHandlers
  }) {
    this.socket = params.socket
    this.serverId = params.serverId
    this.cols = params.cols
    this.rows = params.rows
    this.handlers = params.handlers
  }

  /** 当前 phase,供 UI 渲染顶部状态条 */
  get currentPhase(): TerminalPhase {
    return this.phase
  }

  /** 已分配的 session_id(open 后才非空) */
  get currentSessionId(): string | null {
    return this.sessionId
  }

  /** 启动:发送 terminal.open,phase 切到 awaiting_open */
  open(): void {
    if (this.phase !== 'idle' && this.phase !== 'closed') return
    if (this.socket.readyState !== WebSocket.OPEN) {
      this.handlers.onError(40003, 'WebSocket 未就绪,无法开启终端')
      this.phase = 'closed'
      return
    }
    this.phase = 'connecting'
    try {
      const frame = wrapFrame('terminal.open', {
        server_id: this.serverId,
        cols: this.cols,
        rows: this.rows
      })
      this.socket.send(JSON.stringify(frame))
      this.phase = 'awaiting_open'
    } catch (err) {
      this.handlers.onError(40003, `terminal.open 发送失败:${String(err)}`)
      this.phase = 'closed'
    }
  }

  /**
   * 由 MonitorWebSocket 的 message 分发器调用,处理 terminal.* 帧。
   * 返回 `true` 表示本帧已消费,分派器不再二次派发。
   */
  handleIncoming(frame: TerminalOpenedFrame | TerminalOutputFrame | unknown): boolean {
    const f = frame as { type?: string; payload?: unknown }
    if (typeof f.type !== 'string' || !f.type.startsWith('terminal.')) return false
    switch (f.type) {
      case 'terminal.opened': {
        if (this.phase !== 'awaiting_open') return true
        const payload = f.payload as TerminalOpenedFrame['payload']
        if (
          !payload ||
          typeof payload.session_id !== 'string' ||
          typeof payload.shell !== 'string'
        ) {
          this.handlers.onError(40003, 'terminal.opened payload 非法')
          this.phase = 'closed'
          return true
        }
        this.sessionId = payload.session_id
        this.phase = 'open'
        this.handlers.onOpened(payload.session_id, payload.shell)
        return true
      }
      case 'terminal.output': {
        const payload = f.payload as TerminalOutputFrame['payload']
        if (!payload || typeof payload.data !== 'string') {
          // 非法 payload 静默丢弃,不破坏会话
          return true
        }
        try {
          this.handlers.onOutput(decodeBase64(payload.data))
        } catch {
          // 解码失败也静默,PTY 输出可能含非法 UTF-8 序列
        }
        return true
      }
      case 'terminal.closed': {
        const payload = f.payload as { reason?: string }
        const reason = typeof payload?.reason === 'string' ? payload.reason : 'closed'
        if (this.phase !== 'closed') {
          this.phase = 'closed'
          this.handlers.onClosed(reason)
        }
        return true
      }
      case 'terminal.error': {
        const payload = f.payload as { code?: number; message?: string }
        const code = typeof payload?.code === 'number' ? payload.code : 50000
        const message = typeof payload?.message === 'string' ? payload.message : 'terminal error'
        this.phase = 'closed'
        this.handlers.onError(code, message)
        return true
      }
      default:
        return true
    }
  }

  /** 发送键盘输入(text 为 UTF-8 字符串;内部 Base64 编码) */
  sendInput(text: string): void {
    if (this.phase !== 'open' || this.sessionId === null) return
    if (text.length === 0) return
    try {
      const frame = wrapFrame('terminal.input', {
        session_id: this.sessionId,
        data: encodeBase64(text)
      })
      this.socket.send(JSON.stringify(frame))
    } catch (err) {
      this.handlers.onError(40003, `terminal.input 发送失败:${String(err)}`)
    }
  }

  /** 调整终端尺寸 */
  resize(cols: number, rows: number): void {
    if (this.phase !== 'open' || this.sessionId === null) return
    if (!Number.isFinite(cols) || !Number.isFinite(rows)) return
    if (cols < 1 || cols > 300 || rows < 1 || rows > 100) return
    try {
      const frame: TerminalResizeFrame = wrapFrame('terminal.resize', {
        session_id: this.sessionId,
        cols,
        rows
      })
      this.socket.send(JSON.stringify(frame))
    } catch {
      // resize 失败不影响会话,留待下次刷新
    }
  }

  /** 主动关闭:发送 terminal.close 并切到 closing;若已 open 后未收到 ack,socket close 兜底 */
  close(): void {
    if (this.phase === 'closed' || this.phase === 'idle' || this.phase === 'closing') return
    if (this.sessionId !== null && this.socket.readyState === WebSocket.OPEN) {
      try {
        const frame: TerminalCloseFrame = wrapFrame('terminal.close', {
          session_id: this.sessionId
        })
        this.socket.send(JSON.stringify(frame))
      } catch {
        // 发送失败无妨,后端会因 socket 断开兜底关闭
      }
    }
    this.phase = 'closing'
  }

  /** 由 MonitorWebSocket.onclose 调用,不再期待任何响应 */
  forceClosed(): void {
    if (this.phase === 'closed') return
    this.phase = 'closed'
  }
}