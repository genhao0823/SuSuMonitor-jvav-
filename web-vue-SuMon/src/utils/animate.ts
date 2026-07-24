/**
 * requestAnimationFrame 驱动的数字滚动动画工具。
 * 任意跨度的整数/浮点数值均可,easeOutCubic 缓动让数字"飞入"目标值。
 */

/**
 * 缓动函数 easeOutCubic:t=0→0,t=1→1,前期快、末段慢。
 */
function easeOutCubic(t: number): number {
  const f = 1 - t
  return 1 - f * f * f
}

/**
 * 从 `from` 滚到 `to`,在过渡期间持续回调当前值。
 *
 * @param from 起始值
 * @param to 目标值
 * @param durationMs 总时长(毫秒)
 * @param onUpdate 每一帧的当前值回调(浮点,调用方按需取整)
 * @param onComplete 结束回调(可选)
 * @returns 取消函数,调用后停止后续帧
 */
export function animateCounter(
  from: number,
  to: number,
  durationMs: number,
  onUpdate: (value: number) => void,
  onComplete?: () => void
): () => void {
  if (durationMs <= 0 || from === to) {
    onUpdate(to)
    onComplete?.()
    return () => undefined
  }

  const startTime = performance.now()
  let rafId = 0
  let cancelled = false

  function tick(now: number): void {
    if (cancelled) {
      return
    }
    const elapsed = now - startTime
    const t = Math.min(1, elapsed / durationMs)
    const eased = easeOutCubic(t)
    const current = from + (to - from) * eased
    onUpdate(current)
    if (t < 1) {
      rafId = requestAnimationFrame(tick)
    } else {
      onComplete?.()
    }
  }

  rafId = requestAnimationFrame(tick)

  return () => {
    cancelled = true
    cancelAnimationFrame(rafId)
  }
}