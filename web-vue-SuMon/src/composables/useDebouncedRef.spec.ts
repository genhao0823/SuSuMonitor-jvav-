import { describe, it, expect, vi } from 'vitest'
import { watch } from 'vue'
import { useDebouncedRef } from '@/composables/useDebouncedRef'

/**
 * useDebouncedRef 单元测试。
 *
 * 覆盖契约:
 * - 初始值立即可读
 * - 连续 set 只触发最后一次,定时器未到时旧值仍可读
 * - delay=0 同步生效(下次 microtask)
 * - watch 能正常订阅 debounced 后的值变更
 */
describe('useDebouncedRef', () => {
  it('初始值立即可读', () => {
    const r = useDebouncedRef<string>('hello', 500)
    expect(r.value).toBe('hello')
  })

  it('连续 set 只触发最后一次;未到 delay 时旧值仍可读', async () => {
    vi.useFakeTimers()
    try {
      const r = useDebouncedRef<string>('a', 200)
      r.value = 'b'
      r.value = 'c'
      r.value = 'd'

      await vi.advanceTimersByTimeAsync(199)
      // 199ms < 200ms,值未提交
      expect(r.value).toBe('a')

      await vi.advanceTimersByTimeAsync(1)
      // 200ms 已到,最终值 'd' 提交
      expect(r.value).toBe('d')
    } finally {
      vi.useRealTimers()
    }
  })

  it('delay=0 时值在 microtask 后立即提交', async () => {
    vi.useFakeTimers()
    try {
      const r = useDebouncedRef<number>(0, 0)
      r.value = 42
      // 同步读仍为旧值
      expect(r.value).toBe(0)
      await vi.advanceTimersByTimeAsync(0)
      // 0ms 后提交
      expect(r.value).toBe(42)
    } finally {
      vi.useRealTimers()
    }
  })

  it('watch 能订阅 debounced 后的值变更', async () => {
    vi.useFakeTimers()
    try {
      const r = useDebouncedRef<string>('init', 100)
      const seen: string[] = []
      watch(r, (v) => {
        seen.push(v)
      })
      r.value = 'first'
      r.value = 'second'
      await vi.advanceTimersByTimeAsync(100)
      // watch 应只触发一次,值为最终值 'second'
      expect(seen).toEqual(['second'])
    } finally {
      vi.useRealTimers()
    }
  })

  it('读操作不重置定时器(避免读抖动)', async () => {
    vi.useFakeTimers()
    try {
      const r = useDebouncedRef<string>('a', 200)
      r.value = 'b'
      // 在 100ms 时刻多次读取,不应推迟 b 的提交时刻
      await vi.advanceTimersByTimeAsync(50)
      expect(r.value).toBe('a')
      await vi.advanceTimersByTimeAsync(50)
      expect(r.value).toBe('a') // 仍 100ms < 200ms
      // 再读取
      void r.value
      void r.value
      await vi.advanceTimersByTimeAsync(100)
      // 200ms 到点,b 提交
      expect(r.value).toBe('b')
    } finally {
      vi.useRealTimers()
    }
  })
})