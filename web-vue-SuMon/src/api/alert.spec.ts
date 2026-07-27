import { describe, it, expect } from 'vitest'
import { isAlertPush } from '@/api/alert'

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