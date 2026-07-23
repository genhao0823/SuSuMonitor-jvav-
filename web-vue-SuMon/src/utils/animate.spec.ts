import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { animateCounter } from '@/utils/animate'

describe('animateCounter', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('duration <= 0 直接调 onUpdate(to)', () => {
    const onUpdate = vi.fn()
    const onComplete = vi.fn()
    animateCounter(0, 10, 0, onUpdate, onComplete)
    expect(onUpdate).toHaveBeenCalledWith(10)
    expect(onComplete).toHaveBeenCalled()
  })

  it('from === to 不调动画,直接完成', () => {
    const onUpdate = vi.fn()
    const onComplete = vi.fn()
    animateCounter(5, 5, 1000, onUpdate, onComplete)
    expect(onUpdate).toHaveBeenCalledWith(5)
    expect(onComplete).toHaveBeenCalled()
  })

  it('返回的 cancel 函数终止后续动画', () => {
    const onUpdate = vi.fn()
    const cancel = animateCounter(0, 100, 600, onUpdate)
    cancel()
    vi.advanceTimersByTime(1000)
    expect(onUpdate).not.toHaveBeenCalled()
  })

  it('正常动画返回 cancel 函数(可调用)', () => {
    const onUpdate = vi.fn()
    const cancel = animateCounter(0, 100, 1200, onUpdate)
    expect(typeof cancel).toBe('function')
    cancel()
  })
})
