import apiClient from '@/api/client'
import type { ApiResponse, CurrentUser } from '@/types/api'

/**
 * 管理员审核模块 API 封装。
 * 字段命名与 OpenAPI admin 标签下的 schemas 保持一致。
 */

/**
 * 调用 GET /api/admin/users/pending 列出待审核用户。
 *
 * @returns 待审核用户数组
 */
export function listPendingUsers(): Promise<ApiResponse<CurrentUser[]>> {
  return apiClient
    .get<ApiResponse<CurrentUser[]>>('/admin/users/pending')
    .then((r) => r.data)
}

/**
 * 调用 PUT /api/admin/users/{id}/approve 通过指定用户。
 *
 * @param id 目标用户 ID
 * @returns 通过后的用户最新状态
 */
export function approveUser(id: number): Promise<ApiResponse<CurrentUser>> {
  return apiClient
    .put<ApiResponse<CurrentUser>>(`/admin/users/${id}/approve`)
    .then((r) => r.data)
}

/**
 * 调用 PUT /api/admin/users/{id}/reject 拒绝指定用户。
 *
 * @param id 目标用户 ID
 * @returns 拒绝后的用户最新状态
 */
export function rejectUser(id: number): Promise<ApiResponse<CurrentUser>> {
  return apiClient
    .put<ApiResponse<CurrentUser>>(`/admin/users/${id}/reject`)
    .then((r) => r.data)
}