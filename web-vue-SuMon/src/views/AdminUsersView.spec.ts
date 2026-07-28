import { describe, it, expect, beforeEach, vi } from 'vitest'
import { nextTick } from 'vue'
import { mount, type VueWrapper } from '@vue/test-utils'
import AdminUsersView from '@/views/AdminUsersView.vue'

/**
 * AdminUsersView 本地搜索框逻辑回归。
 *
 * 范围:
 * - searchKeyword 空 → 表格 :data 透传原 pendingList
 * - searchKeyword 非空 → 表格 :data 只包含 username 包含关键字(大小写不敏感)的项
 * - 空结果时 empty-text 切换文案
 *
 * 策略:stub PageHeader 与 Element Plus 子组件避免 jsdom 警告,
 * 直接 mock @/api/admin.listPendingUsers 返回固定 3 条数据。
 */

vi.mock('@/api/admin', () => ({
  listPendingUsers: vi.fn().mockResolvedValue({
    code: 0,
    message: 'success',
    data: [
      { id: 1, username: 'alice', role: 'user', reviewStatus: 'pending', reviewedAt: null, createdAt: '2026-07-20T00:00:00Z' },
      { id: 2, username: 'bob',   role: 'user', reviewStatus: 'pending', reviewedAt: null, createdAt: '2026-07-21T00:00:00Z' },
      { id: 3, username: 'Alex',  role: 'user', reviewStatus: 'pending', reviewedAt: null, createdAt: '2026-07-22T00:00:00Z' }
    ]
  }),
  approveUser: vi.fn().mockResolvedValue({ code: 0, message: 'success', data: null }),
  rejectUser: vi.fn().mockResolvedValue({ code: 0, message: 'success', data: null })
}))

const globalStubs = {
  PageHeader: { template: '<div class="page-header-stub" />' },
  'el-input': {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template:
      '<input class="el-input-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
  },
  'el-table': {
    props: ['data', 'emptyText'],
    template: '<div class="el-table-stub" :data-rows="JSON.stringify(data)" :empty-text="emptyText" />'
  },
  'el-table-column': { template: '<div class="el-table-column-stub"><slot /></div>' },
  'el-button': { template: '<button class="el-button-stub"><slot /></button>' },
  'el-popconfirm': { template: '<div class="el-popconfirm-stub"><slot /></div>' },
  'el-tag': { template: '<span class="el-tag-stub"><slot /></span>' },
  'el-card': { template: '<div class="el-card-stub"><slot /></div>' }
}

async function flush(): Promise<void> {
  await nextTick()
  await Promise.resolve()
}

describe('AdminUsersView 本地搜索', () => {
  let wrapper: VueWrapper

  beforeEach(async () => {
    wrapper = mount(AdminUsersView, {
      global: { stubs: globalStubs }
    })
    await flush()
  })

  it('初始无关键字:表格 :data 含全部 3 条', () => {
    const table = wrapper.find('.el-table-stub')
    const rows = JSON.parse(table.attributes('data-rows') ?? '[]') as Array<{ username: string }>
    expect(rows).toHaveLength(3)
    expect(rows.map((r) => r.username)).toEqual(['alice', 'bob', 'Alex'])
  })

  it('搜索 "al":命中 alice 和 Alex(大小写不敏感)', async () => {
    const input = wrapper.find('.el-input-stub')
    await input.setValue('al')
    await flush()
    const table = wrapper.find('.el-table-stub')
    const rows = JSON.parse(table.attributes('data-rows') ?? '[]') as Array<{ username: string }>
    expect(rows.map((r) => r.username).sort()).toEqual(['Alex', 'alice'])
  })

  it('搜索 "bob":只命中 bob', async () => {
    const input = wrapper.find('.el-input-stub')
    await input.setValue('bob')
    await flush()
    const table = wrapper.find('.el-table-stub')
    const rows = JSON.parse(table.attributes('data-rows') ?? '[]') as Array<{ username: string }>
    expect(rows.map((r) => r.username)).toEqual(['bob'])
  })

  it('搜索 "zzz":空结果,empty-text 切换为"无匹配用户"', async () => {
    const input = wrapper.find('.el-input-stub')
    await input.setValue('zzz')
    await flush()
    const table = wrapper.find('.el-table-stub')
    const rows = JSON.parse(table.attributes('data-rows') ?? '[]') as unknown[]
    expect(rows).toHaveLength(0)
    expect(table.attributes('empty-text')).toBe('无匹配用户')
  })

  it('搜索清空后:表格恢复原 3 条 + empty-text 恢复', async () => {
    const input = wrapper.find('.el-input-stub')
    await input.setValue('bob')
    await flush()
    await input.setValue('')
    await flush()
    const table = wrapper.find('.el-table-stub')
    const rows = JSON.parse(table.attributes('data-rows') ?? '[]') as unknown[]
    expect(rows).toHaveLength(3)
    expect(table.attributes('empty-text')).toBe('暂无待审核用户,所有申请已处理完毕')
  })

  it('关键字前后空格:被 trim 后再匹配', async () => {
    const input = wrapper.find('.el-input-stub')
    await input.setValue('  bob  ')
    await flush()
    const table = wrapper.find('.el-table-stub')
    const rows = JSON.parse(table.attributes('data-rows') ?? '[]') as Array<{ username: string }>
    expect(rows.map((r) => r.username)).toEqual(['bob'])
  })
})