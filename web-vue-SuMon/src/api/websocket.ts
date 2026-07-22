import apiClient from '@/api/client'
import type { ApiResponse } from '@/types/api'

interface MonitorTicket {
  ticket: string
  expires_at: string
}

/** 为浏览器 Monitor WebSocket 获取短时一次性 ticket。 */
export function issueMonitorTicket(): Promise<ApiResponse<MonitorTicket>> {
  return apiClient.post<ApiResponse<MonitorTicket>>('/ws/monitor-ticket').then((response) => response.data)
}
