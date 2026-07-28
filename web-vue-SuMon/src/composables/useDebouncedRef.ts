import { customRef, type Ref } from 'vue'

/**
 * 把 ref 的写入延迟 delay 毫秒后再提交,连续写入只触发最后一次。
 *
 * 设计取舍:
 * - 读(get)同步返回当前值,且 track 当前依赖;读不重置定时器,避免读抖动
 * - 写(set)总是清掉旧定时器,新建一个 delay 后触发的定时器;若期间有写入则被合并
 * - 不使用 watchEffect / watchDebounced 等大块 API,保持 composable 单一职责且零依赖
 *
 * 与 lodash.debounce 的区别:debounce 返回函数;本工具返回标准 Ref<T>,
 * 可直接用于 template `v-model` 与 setup script `watch`。
 *
 * @example
 * ```ts
 * const keyword = useDebouncedRef<string>('', 500)
 * keyword.value = 'web'  // 500ms 后内部值才更新,触发 watch
 * watch(keyword, (v) => { reload(v) })
 * ```
 *
 * @param initial 初始值
 * @param delay 延迟毫秒,默认 500
 * @returns 带 debounce 的 Ref<T>
 */
export function useDebouncedRef<T>(initial: T, delay = 500): Ref<T> {
  let timer: ReturnType<typeof setTimeout> | null = null
  return customRef<T>((track, trigger) => {
    let value = initial
    return {
      get(): T {
        track()
        return value
      },
      set(newValue: T): void {
        if (timer !== null) {
          clearTimeout(timer)
        }
        timer = setTimeout(() => {
          value = newValue
          trigger()
          timer = null
        }, delay)
      }
    }
  })
}