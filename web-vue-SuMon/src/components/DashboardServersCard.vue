<template>
  <el-card
    class="dashboard-view__card dashboard-view__card--glass"
    shadow="never"
  >
    <template #header>
      <div class="dashboard-view__card-header">
        <div class="dashboard-view__card-title">
          <TushanFoxMark
            :size="32"
            alt="涂山苏苏·服务器"
          />
          服务器总数
        </div>
        <span class="dashboard-view__badge dashboard-view__badge--info">
          {{ count }}
        </span>
      </div>
    </template>
    <el-skeleton
      v-if="loading"
      :rows="3"
      animated
    />
    <template v-else>
      <div class="dashboard-view__card-value dashboard-view__card-value--accent">
        {{ displayedTotal }}
      </div>
      <div class="dashboard-view__spark-wrap">
        <svg
          class="dashboard-view__spark"
          viewBox="0 0 100 30"
          preserveAspectRatio="none"
          aria-hidden="true"
        >
          <defs>
            <linearGradient
              id="sparkFill"
              x1="0"
              x2="0"
              y1="0"
              y2="1"
            >
              <stop
                offset="0%"
                stop-color="#ff5b8a"
                stop-opacity="0.35"
              />
              <stop
                offset="100%"
                stop-color="#ff5b8a"
                stop-opacity="0"
              />
            </linearGradient>
          </defs>
          <path
            :d="sparkAreaPath"
            fill="url(#sparkFill)"
          />
          <polyline
            :points="sparkLinePoints"
            fill="none"
            stroke="#ff5b8a"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        <div class="dashboard-view__spark-meta">
          <span class="dashboard-view__spark-label">服务器总数趋势(模拟)</span>
          <span class="dashboard-view__spark-delta">+{{ sparkDelta }}</span>
        </div>
      </div>
    </template>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import TushanFoxMark from '@/components/TushanFoxMark.vue'

/**
 * Dashboard 服务器总数卡(含 spark line 趋势图)。
 *
 * @prop count 后端真实总数
 * @prop displayedTotal 滚动显示当前帧值
 * @prop loading 是否在加载
 */

const props = defineProps<{
  count: number
  displayedTotal: number
  loading: boolean
}>()

/**
 * spark line 用的服务器总数历史值(模拟数据,7 个点)。
 * 以 displayedTotal 为终点构造"近期缓步上升"的曲线。
 */
const sparkData = computed<number[]>(() => {
  const target = Math.max(props.displayedTotal, 0)
  return [
    Math.max(target - 4, 0),
    Math.max(target - 3, 0),
    Math.max(target - 2, 0),
    Math.max(target - 2, 0),
    Math.max(target - 1, 0),
    Math.max(target - 1, 0),
    target
  ]
})

/**
 * spark line 折线点字符串(viewBox 0..100 宽,0..30 高)。
 */
const sparkLinePoints = computed<string>(() => {
  const data = sparkData.value
  const max = Math.max(...data, 1)
  const min = Math.min(...data, 0)
  const range = Math.max(max - min, 1)
  const stepX = 100 / (data.length - 1)
  return data
    .map((v, i) => `${(i * stepX).toFixed(1)},${(30 - ((v - min) / range) * 26 - 2).toFixed(1)}`)
    .join(' ')
})

/**
 * spark line 填充区域(折线到 30 高度的闭合)。
 */
const sparkAreaPath = computed<string>(() => {
  const points = sparkLinePoints.value.split(' ').filter(Boolean)
  if (points.length === 0) {
    return ''
  }
  return `M0,30 L${points.join(' L')} L100,30 Z`
})

/**
 * spark line 末尾与首位的差值,显示"+n"作为趋势标签。
 */
const sparkDelta = computed<number>(() => {
  const data = sparkData.value
  if (data.length < 2) {
    return 0
  }
  return data[data.length - 1] - data[0]
})
</script>