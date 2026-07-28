import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { TerminalWebSocket, type TerminalHandlers } from '@/services/terminal-ws'

/**
 * TerminalWebSocket 单元测试。
 *
 * 策略:用一个最小可用的 WebSocket mock(仅实现 readyState + send + 暴露
 * onmessage setter),覆盖协议层的关键不变量。
 */

/** 内部保留最近一次 send 的字符串(模拟 send 缓冲)。 */
class FakeWebSocket {
  static readonly OPEN = 1
  static readonly CLOSED = 3

  readyState: number = FakeWebSocket.OPEN
  sent: string[] = []
  onopen: ((ev: Event) => void) | null = null
  onmessage: ((ev: { data: string }) => void) | null = null
  onclose: ((ev: CloseEvent) => void) | null = null

  send(data: string): void {
    if (this.readyState !== FakeWebSocket.OPEN) {
      throw new Error('socket not open')
    }
    this.sent.push(data)
  }

  close(): void {
    this.readyState = FakeWebSocket.CLOSED
    this.onclose?.(new CloseEvent('close'))
  }

  /** 测试辅助:模拟服务端下推一帧。 */
  emit(payload: unknown): void {
    this.onmessage?.({ data: JSON.stringify(payload) })
  }

  /** 测试辅助:查找最后一次发出的特定 type 帧的 payload。 */
  lastPayload<T = Record<string, unknown>>(type: string): T | undefined {
    for (let i = this.sent.length - 1; i >= 0; i--) {
      const parsed = JSON.parse(this.sent[i]) as { type: string; payload: unknown }
      if (parsed.type === type) return parsed.payload as T
    }
    return undefined
  }
}

let socket: FakeWebSocket
let handlers: TerminalHandlers

beforeEach(() => {
  socket = new FakeWebSocket()
  handlers = {
    onOpened: vi.fn(),
    onOutput: vi.fn(),
    onClosed: vi.fn(),
    onError: vi.fn()
  }
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('TerminalWebSocket', () => {
  it('open() 发送 terminal.open 帧并切到 awaiting_open', () => {
    const ws = new TerminalWebSocket({
      socket: socket as unknown as WebSocket,
      serverId: 7,
      cols: 120,
      rows: 30,
      handlers
    })
    ws.open()
    expect(ws.currentPhase).toBe('awaiting_open')
    const payload = socket.lastPayload<{ server_id: number; cols: number; rows: number }>(
      'terminal.open'
    )
    expect(payload).toEqual({ server_id: 7, cols: 120, rows: 30 })
  })

  it('open() 在 socket 未 OPEN 时调用 onError(40003) 并保持 closed', () => {
    socket.readyState = FakeWebSocket.CLOSED
    const ws = new TerminalWebSocket({
      socket: socket as unknown as WebSocket,
      serverId: 1,
      cols: 80,
      rows: 24,
      handlers
    })
    ws.open()
    expect(handlers.onError).toHaveBeenCalledWith(40003, expect.any(String))
    expect(ws.currentPhase).toBe('closed')
  })

  it('handleIncoming: terminal.opened 触发 onOpened 并切到 open', () => {
    const ws = new TerminalWebSocket({
      socket: socket as unknown as WebSocket,
      serverId: 1,
      cols: 80,
      rows: 24,
      handlers
    })
    ws.open()
    ws.handleIncoming({
      type: 'terminal.opened',
      message_id: 'm1',
      timestamp: '2026-07-28T00:00:00Z',
      payload: { server_id: 1, session_id: 'sess-abc', shell: '/bin/bash' }
    })
    expect(handlers.onOpened).toHaveBeenCalledWith('sess-abc', '/bin/bash')
    expect(ws.currentPhase).toBe('open')
    expect(ws.currentSessionId).toBe('sess-abc')
  })

  it('handleIncoming: terminal.output Base64 解码后回调 onOutput', () => {
    const ws = new TerminalWebSocket({
      socket: socket as unknown as WebSocket,
      serverId: 1,
      cols: 80,
      rows: 24,
      handlers
    })
    ws.open()
    ws.handleIncoming({
      type: 'terminal.opened',
      message_id: 'm1',
      timestamp: '2026-07-28T00:00:00Z',
      payload: { server_id: 1, session_id: 'sess-1', shell: '/bin/bash' }
    })
    // "hello\n" 的 Base64 = "aGVsbG8K"
    ws.handleIncoming({
      type: 'terminal.output',
      message_id: 'm2',
      timestamp: '2026-07-28T00:00:01Z',
      payload: { server_id: 1, session_id: 'sess-1', data: btoa('hello\n') }
    })
    expect(handlers.onOutput).toHaveBeenCalledWith('hello\n')
  })

  it('handleIncoming: terminal.closed 触发 onClosed 并切到 closed', () => {
    const ws = new TerminalWebSocket({
      socket: socket as unknown as WebSocket,
      serverId: 1,
      cols: 80,
      rows: 24,
      handlers
    })
    ws.open()
    ws.handleIncoming({
      type: 'terminal.opened',
      message_id: 'm1',
      timestamp: '2026-07-28T00:00:00Z',
      payload: { server_id: 1, session_id: 'sess-1', shell: '/bin/bash' }
    })
    ws.handleIncoming({
      type: 'terminal.closed',
      message_id: 'm3',
      timestamp: '2026-07-28T00:00:02Z',
      payload: { server_id: 1, session_id: 'sess-1', reason: 'agent_disconnected' }
    })
    expect(handlers.onClosed).toHaveBeenCalledWith('agent_disconnected')
    expect(ws.currentPhase).toBe('closed')
  })

  it('handleIncoming: terminal.error 触发 onError 并切到 closed', () => {
    const ws = new TerminalWebSocket({
      socket: socket as unknown as WebSocket,
      serverId: 1,
      cols: 80,
      rows: 24,
      handlers
    })
    ws.open()
    ws.handleIncoming({
      type: 'terminal.opened',
      message_id: 'm1',
      timestamp: '2026-07-28T00:00:00Z',
      payload: { server_id: 1, session_id: 'sess-1', shell: '/bin/bash' }
    })
    ws.handleIncoming({
      type: 'terminal.error',
      message_id: 'm4',
      timestamp: '2026-07-28T00:00:03Z',
      payload: { server_id: 1, session_id: 'sess-1', code: 40904, message: 'agent offline' }
    })
    expect(handlers.onError).toHaveBeenCalledWith(40904, 'agent offline')
    expect(ws.currentPhase).toBe('closed')
  })

  it('handleIncoming: 非 terminal.* 帧返回 false(不消费)', () => {
    const ws = new TerminalWebSocket({
      socket: socket as unknown as WebSocket,
      serverId: 1,
      cols: 80,
      rows: 24,
      handlers
    })
    const consumed = ws.handleIncoming({
      type: 'metrics.update',
      message_id: 'm1',
      timestamp: '2026-07-28T00:00:00Z',
      payload: { server_id: 1, metrics: {} }
    })
    expect(consumed).toBe(false)
    expect(handlers.onOutput).not.toHaveBeenCalled()
    expect(handlers.onOpened).not.toHaveBeenCalled()
  })

  it('sendInput 在 open 之前不发送;open 后正确 Base64 编码并发送', () => {
    const ws = new TerminalWebSocket({
      socket: socket as unknown as WebSocket,
      serverId: 1,
      cols: 80,
      rows: 24,
      handlers
    })
    ws.sendInput('ls\n')
    expect(socket.sent).toHaveLength(0)

    ws.open()
    ws.handleIncoming({
      type: 'terminal.opened',
      message_id: 'm1',
      timestamp: '2026-07-28T00:00:00Z',
      payload: { server_id: 1, session_id: 'sess-1', shell: '/bin/bash' }
    })
    ws.sendInput('ls\n')
    const payload = socket.lastPayload<{ session_id: string; data: string }>('terminal.input')
    expect(payload?.session_id).toBe('sess-1')
    expect(payload?.data).toBe(btoa('ls\n'))
  })

  it('resize 越界不发,合法值发送 terminal.resize', () => {
    const ws = new TerminalWebSocket({
      socket: socket as unknown as WebSocket,
      serverId: 1,
      cols: 80,
      rows: 24,
      handlers
    })
    ws.open()
    ws.handleIncoming({
      type: 'terminal.opened',
      message_id: 'm1',
      timestamp: '2026-07-28T00:00:00Z',
      payload: { server_id: 1, session_id: 'sess-1', shell: '/bin/bash' }
    })
    ws.resize(0, 24) // 非法
    ws.resize(80, 200) // rows 越界
    ws.resize(100, 40) // 合法
    const payload = socket.lastPayload<{ session_id: string; cols: number; rows: number }>(
      'terminal.resize'
    )
    expect(payload).toEqual({ session_id: 'sess-1', cols: 100, rows: 40 })
  })

  it('close() 发 terminal.close 并切到 closing,二次调用幂等', () => {
    const ws = new TerminalWebSocket({
      socket: socket as unknown as WebSocket,
      serverId: 1,
      cols: 80,
      rows: 24,
      handlers
    })
    ws.open()
    ws.handleIncoming({
      type: 'terminal.opened',
      message_id: 'm1',
      timestamp: '2026-07-28T00:00:00Z',
      payload: { server_id: 1, session_id: 'sess-1', shell: '/bin/bash' }
    })
    ws.close()
    ws.close() // 二次幂等
    expect(ws.currentPhase).toBe('closing')
    const payload = socket.lastPayload<{ session_id: string }>('terminal.close')
    expect(payload).toEqual({ session_id: 'sess-1' })
    // 第二次调用不应多发
    const closeCount = socket.sent.filter((raw) => {
      const parsed = JSON.parse(raw) as { type: string }
      return parsed.type === 'terminal.close'
    }).length
    expect(closeCount).toBe(1)
  })

  it('forceClosed 由 MonitorWebSocket.onclose 调用时直接切到 closed 不发 close 帧', () => {
    const ws = new TerminalWebSocket({
      socket: socket as unknown as WebSocket,
      serverId: 1,
      cols: 80,
      rows: 24,
      handlers
    })
    ws.open()
    ws.handleIncoming({
      type: 'terminal.opened',
      message_id: 'm1',
      timestamp: '2026-07-28T00:00:00Z',
      payload: { server_id: 1, session_id: 'sess-1', shell: '/bin/bash' }
    })
    const sentBefore = socket.sent.length
    ws.forceClosed()
    expect(ws.currentPhase).toBe('closed')
    expect(socket.sent.length).toBe(sentBefore)
  })
})