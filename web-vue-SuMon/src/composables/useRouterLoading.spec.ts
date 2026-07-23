import { describe, it, expect, beforeEach, vi } from 'vitest'
import { installRouterLoading } from '@/composables/useRouterLoading'
import type { Router } from 'vue-router'

vi.mock('nprogress', () => ({
  default: {
    configure: vi.fn(),
    start: vi.fn(),
    done: vi.fn()
  }
}))

import NProgress from 'nprogress'

function makeRouter(handlers: { [k: string]: Array<() => unknown> }): Router {
  return {
    beforeEach: (fn: () => unknown) => handlers.beforeEach.push(fn),
    afterEach: (fn: () => unknown) => handlers.afterEach.push(fn),
    onError: (fn: () => unknown) => handlers.onError.push(fn)
  } as unknown as Router
}

describe('installRouterLoading', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('注册了 3 个 router hook', () => {
    const handlers: { [k: string]: Array<() => unknown> } = { beforeEach: [], afterEach: [], onError: [] }
    installRouterLoading(makeRouter(handlers))
    expect(handlers.beforeEach).toHaveLength(1)
    expect(handlers.afterEach).toHaveLength(1)
    expect(handlers.onError).toHaveLength(1)
  })

  it('beforeEach 触发 NProgress.start', () => {
    const handlers: { [k: string]: Array<() => unknown> } = { beforeEach: [], afterEach: [], onError: [] }
    installRouterLoading(makeRouter(handlers))
    handlers.beforeEach[0]()
    expect(NProgress.start).toHaveBeenCalled()
  })

  it('afterEach 和 onError 都触发 NProgress.done', () => {
    const handlers: { [k: string]: Array<() => unknown> } = { beforeEach: [], afterEach: [], onError: [] }
    installRouterLoading(makeRouter(handlers))
    handlers.afterEach[0]()
    expect(NProgress.done).toHaveBeenCalledTimes(1)
    handlers.onError[0]()
    expect(NProgress.done).toHaveBeenCalledTimes(2)
  })
})
