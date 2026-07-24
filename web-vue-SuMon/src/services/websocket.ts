import type { MetricsLatest } from '@/types/metrics'
import { issueMonitorTicket } from '@/api/websocket'

interface MonitorMessage {
  type: string
  payload?: { server_id?: number; metrics?: MetricsLatest }
}

/** 浏览器 Monitor WebSocket 客户端，使用一次性 ticket，不把长期 JWT 放入 URL。 */
export class MonitorWebSocket {
  private socket: WebSocket | null = null
  private retryTimer: ReturnType<typeof setTimeout> | null = null
  private stopped = false

  constructor(
    private readonly onMetrics: (value: MetricsLatest) => void,
    private readonly onConnected: (value: boolean) => void
  ) {}

  connect(serverId: number): void {
    this.stopped = false
    void this.open(serverId)
  }

  disconnect(): void {
    this.stopped = true
    if (this.retryTimer !== null) clearTimeout(this.retryTimer)
    this.socket?.close()
    this.socket = null
    this.onConnected(false)
  }

  private async open(serverId: number): Promise<void> {
    if (this.stopped) return
    let ticket: string
    try {
      const response = await issueMonitorTicket()
      if (!response.data) throw new Error('Monitor ticket missing')
      ticket = response.data.ticket
    } catch {
      this.onConnected(false)
      if (!this.stopped) this.retryTimer = setTimeout(() => void this.open(serverId), 3000)
      return
    }
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    this.socket = new WebSocket(`${protocol}//${window.location.host}/ws/monitor?ticket=${encodeURIComponent(ticket)}`)
    this.socket.onopen = () => {
      this.onConnected(true)
      this.socket?.send(JSON.stringify({
        type: 'metrics.subscribe',
        message_id: crypto.randomUUID(),
        payload: { server_id: serverId }
      }))
    }
    this.socket.onmessage = (event) => {
      const message = JSON.parse(event.data) as MonitorMessage
      if (message.type === 'metrics.update' && message.payload?.metrics) {
        this.onMetrics(message.payload.metrics)
      }
    }
    this.socket.onclose = () => {
      this.onConnected(false)
      if (!this.stopped) {
          this.retryTimer = setTimeout(() => void this.open(serverId), 3000)
      }
    }
  }
}
