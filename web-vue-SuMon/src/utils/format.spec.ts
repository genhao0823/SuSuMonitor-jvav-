import { describe, it, expect } from 'vitest'
import {
  formatDateTime,
  serverStatusLabel,
  userRoleLabel,
  reviewStatusLabel
} from '@/utils/format'

describe('format utilities', () => {
  it('formatDateTime(null) 返回 "-"', () => {
    expect(formatDateTime(null)).toBe('-')
  })

  it('formatDateTime(undefined) 返回 "-"', () => {
    expect(formatDateTime(undefined)).toBe('-')
  })

  it('formatDateTime("invalid") 返回 "-"', () => {
    expect(formatDateTime('not-a-date')).toBe('-')
  })

  it('formatDateTime(ISO) 返回 YYYY-MM-DD HH:mm:ss', () => {
    expect(formatDateTime('2026-07-22T15:30:00Z')).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)
  })

  it('serverStatusLabel(online/offline/unknown) → 中文', () => {
    expect(serverStatusLabel('online')).toBe('在线')
    expect(serverStatusLabel('offline')).toBe('离线')
    expect(serverStatusLabel('unknown')).toBe('未知')
  })

  it('userRoleLabel(admin/user) → 管理员/普通用户', () => {
    expect(userRoleLabel('admin')).toBe('管理员')
    expect(userRoleLabel('user')).toBe('普通用户')
  })

  it('reviewStatusLabel(approved/rejected/pending) → 已通过/已拒绝/待审核', () => {
    expect(reviewStatusLabel('approved')).toBe('已通过')
    expect(reviewStatusLabel('rejected')).toBe('已拒绝')
    expect(reviewStatusLabel('pending')).toBe('待审核')
  })

  it('serverStatusTagType(online) → success', () => {
    import('@/utils/format').then(({ serverStatusTagType, reviewStatusTagType }) => {
      expect(serverStatusTagType('online')).toBe('success')
      expect(serverStatusTagType('offline')).toBe('info')
      expect(serverStatusTagType('unknown')).toBe('warning')
      expect(reviewStatusTagType('approved')).toBe('success')
      expect(reviewStatusTagType('rejected')).toBe('info')
      expect(reviewStatusTagType('pending')).toBe('warning')
    })
  })
})
