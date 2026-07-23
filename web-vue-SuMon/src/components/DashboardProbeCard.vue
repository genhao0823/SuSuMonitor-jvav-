<template>
  <el-card
    class="probe-card dashboard-view__card dashboard-view__card--glass"
    shadow="never"
  >
    <template #header>
      <div class="probe-card__header dashboard-view__card-header">
        <div class="probe-card__title dashboard-view__card-title">
          <TushanFoxMark
            :size="32"
            :alt="`涂山苏苏·${title}`"
          />
          <span
            v-if="ok && pulse"
            class="dashboard-view__pulse"
            aria-hidden="true"
          />
          {{ title }}
        </div>
        <span
          class="dashboard-view__badge"
          :class="ok ? 'dashboard-view__badge--ok' : 'dashboard-view__badge--down'"
        >
          {{ ok ? okLabel : 'DOWN' }}
        </span>
      </div>
    </template>
    <el-skeleton
      v-if="loading"
      :rows="2"
      animated
    />
    <template v-else>
      <div class="dashboard-view__card-value">
        {{ ok ? detail : '后端不可达' }}
      </div>
      <div class="dashboard-view__card-hint">
        {{ hint }}
      </div>
    </template>
  </el-card>
</template>

<script setup lang="ts">
import TushanFoxMark from '@/components/TushanFoxMark.vue'

/**
 * Dashboard 通用探针卡(健康检查 / 就绪检查共用)。
 *
 * @prop title 卡片标题
 * @prop ok 是否通过
 * @prop detail 探针详情文本
 * @prop hint 卡片底部 hint
 * @prop loading 是否在加载
 * @prop pulse 是否显示脉冲点(健康卡专用)
 * @prop okLabel 通过时的徽标文字(UP / READY)
 */

defineProps<{
  title: string
  ok: boolean
  detail: string
  hint: string
  loading: boolean
  pulse?: boolean
  okLabel: string
}>()
</script>

<style scoped>
.probe-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.probe-card__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #2a1626;
}

@media (max-width: 720px) {
  .probe-card__title {
    font-size: 12px;
  }
}
</style>