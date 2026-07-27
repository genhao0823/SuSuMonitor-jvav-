import { describe, it, expect, vi } from 'vitest'
import apiClient from '@/api/client'
import {
  isAlertPush,
  listAlertRules,
  createAlertRule,
  updateAlertRule,
  deleteAlertRule,
  listAlertRecords,
  markAlertRecordAsRead
} from '@/api/alert'
import type {
  AlertRecord,
  AlertRule,
  PageResult
} from '@/types/api'

describe('alert api / isAlertPush', () => {
  it('完整合法 payload 命中 true', () => {
    const payload = {
      server_id: 1,
      alert: {
        id: 42,
        rule_id: 2,
        metric: 'cpu',
        current_value: 90.5,
        threshold_value: 80,
        level: 'warning',
        status: 'unread',
        triggered_at: '2026-07-22T00:00:00Z'
      }
    }
    expect(isAlertPush(payload)).toBe(true)
  })

  it('非对象 payload 命中 false', () => {
    expect(isAlertPush(null)).toBe(false)
    expect(isAlertPush(undefined)).toBe(false)
    expect(isAlertPush(42)).toBe(false)
    expect(isAlertPush('alert.push')).toBe(false)
    expect(isAlertPush([])).toBe(false)
  })

  it('缺 server_id 命中 false', () => {
    const payload = {
      alert: {
        id: 1,
        metric: 'cpu',
        triggered_at: '2026-07-22T00:00:00Z'
      }
    }
    expect(isAlertPush(payload)).toBe(false)
  })

  it('alert 缺 id 命中 false', () => {
    const payload = {
      server_id: 1,
      alert: {
        metric: 'cpu',
        triggered_at: '2026-07-22T00:00:00Z'
      }
    }
    expect(isAlertPush(payload)).toBe(false)
  })

  it('alert 字段类型错误命中 false', () => {
    const payload = {
      server_id: '1',
      alert: {
        id: 1,
        metric: 'cpu',
        triggered_at: '2026-07-22T00:00:00Z'
      }
    }
    expect(isAlertPush(payload)).toBe(false)
  })
})

/**
 * HTTP wrapper 单测:聚焦 URL / Method / params / body 三件事,
 * 不重复 axios 内部行为。
 */
describe('alert api / HTTP wrappers', () => {
  it('listAlertRules 调 GET /alerts/rules', async () => {
    const spy = vi.spyOn(apiClient, 'get').mockResolvedValue({
      data: { code: 0, message: 'success', data: [] as AlertRule[] }
    })
    const result = await listAlertRules()
    expect(spy).toHaveBeenCalledWith('/alerts/rules')
    expect(result.data).toEqual([])
  })

  it('createAlertRule 调 POST /alerts/rules 与 body', async () => {
    const spy = vi.spyOn(apiClient, 'post').mockResolvedValue({
      data: { code: 0, message: 'success', data: { id: 1 } as AlertRule }
    })
    const body = {
      server_id: null,
      metric: 'cpu' as const,
      operator: '>' as const,
      threshold_value: 80,
      level: 'warning' as const
    }
    await createAlertRule(body)
    expect(spy).toHaveBeenCalledWith('/alerts/rules', body)
  })

  it('updateAlertRule 调 PUT /alerts/rules/{id} 与 body', async () => {
    const spy = vi.spyOn(apiClient, 'put').mockResolvedValue({
      data: { code: 0, message: 'success', data: { id: 7 } as AlertRule }
    })
    const body = { threshold_value: 90, level: 'critical' as const, enabled: false }
    await updateAlertRule(7, body)
    expect(spy).toHaveBeenCalledWith('/alerts/rules/7', body)
  })

  it('deleteAlertRule 调 DELETE /alerts/rules/{id}', async () => {
    const spy = vi.spyOn(apiClient, 'delete').mockResolvedValue({
      data: { code: 0, message: 'success', data: null }
    })
    await deleteAlertRule(7)
    expect(spy).toHaveBeenCalledWith('/alerts/rules/7')
  })

  it('listAlertRecords 调 GET /alerts/records 与 query params', async () => {
    const spy = vi.spyOn(apiClient, 'get').mockResolvedValue({
      data: { code: 0, message: 'success', data: { items: [], total: 0, page: 2, page_size: 30 } as PageResult<AlertRecord> }
    })
    await listAlertRecords({ page: 2, page_size: 30, server_id: 5, status: 'unread' })
    expect(spy).toHaveBeenCalledWith('/alerts/records', {
      params: { page: 2, page_size: 30, server_id: 5, status: 'unread' }
    })
  })

  it('markAlertRecordAsRead 调 PUT /alerts/records/{id}/read', async () => {
    const spy = vi.spyOn(apiClient, 'put').mockResolvedValue({
      data: { code: 0, message: 'success', data: null }
    })
    await markAlertRecordAsRead(99)
    expect(spy).toHaveBeenCalledWith('/alerts/records/99/read')
  })
})
