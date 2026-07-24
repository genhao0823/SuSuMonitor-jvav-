import apiClient from '@/api/client'
import type { ApiResponse, HealthStatus, ReadyStatus } from '@/types/api'

/**
 * 调用 /api/health 获取后端存活状态。
 *
 * @returns 健康状态数据
 */
export function getHealth(): Promise<ApiResponse<HealthStatus>> {
  return apiClient.get<ApiResponse<HealthStatus>>('/health').then((r) => r.data)
}

/**
 * 调用 /api/ready 获取后端与数据库的就绪状态。
 *
 * @returns 就绪状态数据
 */
export function getReady(): Promise<ApiResponse<ReadyStatus>> {
  return apiClient.get<ApiResponse<ReadyStatus>>('/ready').then((r) => r.data)
}