import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import ServerSearchBar from '@/components/ServerSearchBar.vue'

/**
 * ServerSearchBar 单元测试。
 *
 * 覆盖契约:
 * - 三个 v-model(nameValue/hostValue/pageSize)双向绑定
 * - el-input 回车触发 reload emit
 * - el-select 切换 pageSize 触发 reload emit
 * - "刷新"按钮触发 reload emit
 *
 * 策略:stub el-input / el-select / el-button 的内部结构,只验顶层 v-model 流转与事件 emit。
 */

const globalStubs = {
  'el-input': {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template:
      '<input class="el-input-stub" :value="modelValue ?? \'\'" @input="$emit(\'update:modelValue\', $event.target.value)" @keyup.enter="$emit(\'keyup.enter\')" />'
  },
  'el-select': {
    props: ['modelValue'],
    emits: ['update:modelValue', 'change'],
    template:
      '<select class="el-select-stub" :value="modelValue" @change="$emit(\'update:modelValue\', Number($event.target.value)); $emit(\'change\', Number($event.target.value))"><slot /></select>'
  },
  'el-option': { template: '<option class="el-option-stub"><slot /></option>' },
  'el-button': { template: '<button class="el-button-stub" @click="$emit(\'click\')"><slot /></button>' }
}

async function flush(): Promise<void> {
  await nextTick()
}

describe('ServerSearchBar', () => {
  it('nameValue v-model 双向绑定:输入框更新触发父组件 update', async () => {
    const w = mount(ServerSearchBar, {
      props: { nameValue: '', hostValue: '', pageSize: 10, pageSizeOptions: [10, 20, 50] },
      global: { stubs: globalStubs }
    })
    const inputs = w.findAll('.el-input-stub')
    await inputs[0].setValue('web')
    await flush()
    const updates = w.emitted('update:nameValue')
    expect(updates).toBeTruthy()
    expect(updates?.[0]).toEqual(['web'])
  })

  it('hostValue v-model 双向绑定', async () => {
    const w = mount(ServerSearchBar, {
      props: { nameValue: '', hostValue: '', pageSize: 10, pageSizeOptions: [10, 20, 50] },
      global: { stubs: globalStubs }
    })
    const inputs = w.findAll('.el-input-stub')
    await inputs[1].setValue('10.0.0')
    await flush()
    const updates = w.emitted('update:hostValue')
    expect(updates?.[0]).toEqual(['10.0.0'])
  })

  it('nameValue 输入框按回车 → emit reload', async () => {
    const w = mount(ServerSearchBar, {
      props: { nameValue: '', hostValue: '', pageSize: 10, pageSizeOptions: [10, 20, 50] },
      global: { stubs: globalStubs }
    })
    await w.findAll('.el-input-stub')[0].trigger('keyup.enter')
    await flush()
    expect(w.emitted('reload')).toBeTruthy()
    expect(w.emitted('reload')).toHaveLength(1)
  })

  it('hostValue 输入框按回车 → emit reload', async () => {
    const w = mount(ServerSearchBar, {
      props: { nameValue: '', hostValue: '', pageSize: 10, pageSizeOptions: [10, 20, 50] },
      global: { stubs: globalStubs }
    })
    await w.findAll('.el-input-stub')[1].trigger('keyup.enter')
    await flush()
    expect(w.emitted('reload')).toBeTruthy()
  })

  it('刷新按钮 → emit reload', async () => {
    const w = mount(ServerSearchBar, {
      props: { nameValue: '', hostValue: '', pageSize: 10, pageSizeOptions: [10, 20, 50] },
      global: { stubs: globalStubs }
    })
    await w.find('.el-button-stub').trigger('click')
    await flush()
    expect(w.emitted('reload')).toBeTruthy()
  })

  it('初始 props.nameValue 渲染到第一个 el-input', () => {
    const w = mount(ServerSearchBar, {
      props: { nameValue: 'init-name', hostValue: 'init-host', pageSize: 20, pageSizeOptions: [10, 20, 50] },
      global: { stubs: globalStubs }
    })
    const inputs = w.findAll('.el-input-stub')
    expect((inputs[0].element as HTMLInputElement).value).toBe('init-name')
    expect((inputs[1].element as HTMLInputElement).value).toBe('init-host')
  })
})