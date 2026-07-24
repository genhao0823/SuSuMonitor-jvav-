import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import DashboardServersCard from '@/components/DashboardServersCard.vue'

/**
 * el-card / el-skeleton 全局组件 stub,绕过"Failed to resolve component" 警告。
 * 不需要完整 Element Plus 环境(单测只验 props 透传 + spark 渲染分支)。
 */
const globalStubs = {
  'el-card': { template: '<div class="el-card-stub"><slot /></div>' },
  'el-skeleton': { template: '<div class="el-skeleton-stub" />' }
}

describe('DashboardServersCard', () => {
  it('mock data 7 个点 → 渲染 count + spark + delta', () => {
    const w = mount(DashboardServersCard, {
      props: { count: 10, data: [5, 6, 7, 8, 9, 10, 11], loading: false },
      global: { stubs: globalStubs }
    })
    expect(w.text()).toContain('10')
    expect(w.find('.server-spark-line').exists()).toBe(true)
    // delta: 11 - 5 = +6
    expect(w.find('.server-spark-line__delta').text()).toContain('+6')
  })

  it('loading=true → 不渲染 spark(显示 el-skeleton-stub)', () => {
    const w = mount(DashboardServersCard, {
      props: { count: 0, data: [], loading: true },
      global: { stubs: globalStubs }
    })
    expect(w.find('.el-skeleton-stub').exists()).toBe(true)
    expect(w.find('.server-spark-line').exists()).toBe(false)
  })

  it('count=0 + data=空 → 显示 0,spark 不渲染 delta', () => {
    const w = mount(DashboardServersCard, {
      props: { count: 0, data: [], loading: false },
      global: { stubs: globalStubs }
    })
    expect(w.text()).toContain('0')
    // data < 2 → spark delta 隐藏
    expect(w.find('.server-spark-line__delta').exists()).toBe(false)
  })

  it('data 单点 → 不渲染 delta', () => {
    const w = mount(DashboardServersCard, {
      props: { count: 5, data: [42], loading: false },
      global: { stubs: globalStubs }
    })
    expect(w.text()).toContain('5')
    expect(w.find('.server-spark-line__delta').exists()).toBe(false)
  })
})
