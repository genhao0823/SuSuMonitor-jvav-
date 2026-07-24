import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useMetricsStore } from '@/stores/metrics'
import type { MetricsLatest } from '@/types/metrics'

function makeMetrics(overrides: Partial<MetricsLatest> = {}): MetricsLatest {
  return {
    server_id: 1,
    cpu_percent: 50,
    memory_percent: 60,
    memory_used: 0,
    memory_total: 0,
    disk_percent: 0,
    disk_used: 0,
    disk_total: 0,
    net_rx: 0,
    net_tx: 0,
    temperature: null,
    load_avg: null,
    collected_at: '2026-07-22',
    ...overrides
  }
}

describe('metrics store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('初始 loading false,latest null', () => {
    const m = useMetricsStore()
    expect(m.loading).toBe(false)
    expect(m.latest).toBeNull()
    expect(m.history).toEqual([])
    expect(m.error).toBeNull()
  })

  it('applyRealtime 覆盖 latest', () => {
    const m = useMetricsStore()
    m.applyRealtime(makeMetrics({ cpu_percent: 99 }))
    expect(m.latest?.cpu_percent).toBe(99)
  })

  it('reset 清空所有 ref', () => {
    const m = useMetricsStore()
    m.applyRealtime(makeMetrics())
    m.reset()
    expect(m.latest).toBeNull()
    expect(m.history).toEqual([])
    expect(m.connected).toBe(false)
  })

  it('connected 状态可设可改', () => {
    const m = useMetricsStore()
    m.setConnected(true)
    expect(m.connected).toBe(true)
    m.setConnected(false)
    expect(m.connected).toBe(false)
  })
})
