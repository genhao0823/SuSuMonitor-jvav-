<template>
  <div class="sush-falling-petals">
    <span
      v-for="(p, i) in petals"
      :key="i"
      :style="p"
      class="sush-falling-petals__petal"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * 涂山苏苏飘落花瓣。
 *
 * 18 朵随机错开延迟,连续不断的飘落流。
 * 由父组件 AuthLayout 通过 `petalCount` 控制密度。
 */

defineProps<{
  petalCount?: number
}>()

interface PetalStyle {
  top: string
  left: string
  size: number
  delay: string
  duration: string
  [key: `--${string}`]: string | number
}

/**
 * 飘落花瓣:18 朵,延迟 0-8.5s 均匀错开,时长 10-13s,
 * 让画面任何时刻同时有 6-9 朵在下落,呈现连续不断的飘落流。
 * 字段顺序与动画 CSS 变量对应: --s(sizes)/ --d(delay)/ --t(duration)
 */
const petals: PetalStyle[] = [
  { top: '5%',  left: '12%', size: 14, delay: '0s',   duration: '11s' },
  { top: '8%',  left: '36%', size: 10, delay: '0.5s', duration: '12s' },
  { top: '4%',  left: '58%', size: 16, delay: '1.0s', duration: '10s' },
  { top: '12%', left: '78%', size: 12, delay: '1.5s', duration: '13s' },
  { top: '20%', left: '24%', size: 9,  delay: '2.0s', duration: '11s' },
  { top: '18%', left: '90%', size: 14, delay: '2.5s', duration: '12s' },
  { top: '32%', left: '4%',  size: 16, delay: '3.0s', duration: '10s' },
  { top: '28%', left: '46%', size: 8,  delay: '3.5s', duration: '13s' },
  { top: '36%', left: '68%', size: 12, delay: '4.0s', duration: '11s' },
  { top: '44%', left: '14%', size: 10, delay: '4.5s', duration: '12s' },
  { top: '48%', left: '82%', size: 14, delay: '5.0s', duration: '10s' },
  { top: '52%', left: '36%', size: 9,  delay: '5.5s', duration: '13s' },
  { top: '58%', left: '60%', size: 16, delay: '6.0s', duration: '11s' },
  { top: '64%', left: '8%',  size: 12, delay: '6.5s', duration: '12s' },
  { top: '70%', left: '88%', size: 14, delay: '7.0s', duration: '10s' },
  { top: '76%', left: '28%', size: 10, delay: '7.5s', duration: '13s' },
  { top: '82%', left: '52%', size: 16, delay: '8.0s', duration: '11s' },
  { top: '88%', left: '74%', size: 12, delay: '8.5s', duration: '12s' }
]
</script>

<style scoped>
.sush-falling-petals {
  position: absolute;
  inset: 0;
  z-index: 4;
  pointer-events: none;
  overflow: hidden;
}

.sush-falling-petals__petal {
  position: absolute;
  width: var(--s);
  height: var(--s);
  background: radial-gradient(circle at 30% 30%, #fff7fa 0%, #ffd0d8 60%, #ffb6c1 100%);
  border-radius: 60% 40% 60% 40%;
  opacity: 0.85;
  animation: sush-falling-petals__fall linear infinite;
  --s: 12px;
  animation-delay: var(--d);
  animation-duration: var(--t);
}

.sush-falling-petals__petal:nth-child(odd) {
  background: radial-gradient(circle at 30% 30%, #fff 0%, #ff7aa3 60%, #b7325c 100%);
}

@keyframes sush-falling-petals__fall {
  0%   { transform: translateY(-20px) rotate(0deg); opacity: 0; }
  10%  { opacity: 0.85; }
  90%  { opacity: 0.85; }
  100% { transform: translateY(120vh) rotate(360deg); opacity: 0; }
}
</style>