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
        {{ count }}
      </div>
      <div class="dashboard-view__spark-wrap">
        <ServerSparkLine
          :data="data"
          label="CPU 7d"
        />
      </div>
    </template>
  </el-card>
</template>

<script setup lang="ts">
import TushanFoxMark from '@/components/TushanFoxMark.vue'
import ServerSparkLine from '@/components/ServerSparkLine.vue'

/**
 * Dashboard 服务器总数卡(Sprint 3:spark 接真实历史)。
 *
 * @prop count 后端真实总数
 * @prop data 7 天 CPU 趋势序列(由父组件 DashboardView 注入)
 * @prop loading 是否在加载
 *
 * Sprint 3 之前:`data` 是 computed 内部 mock 7 个点
 * Sprint 3 之后:`data` 由父组件拉真实 `/api/servers/{id}/metrics` 后传入
 * spark 渲染复用通用 ServerSparkLine 组件(消除 2 处重复实现)
 */

defineProps<{
  count: number
  data: number[]
  loading: boolean
}>()
</script>
