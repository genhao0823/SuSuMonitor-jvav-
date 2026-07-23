<template>
  <el-card
    class="dashboard-view__card dashboard-view__card--glass"
    shadow="never"
  >
    <slot />
  </el-card>
</template>

<script setup lang="ts">
/**
 * Dashboard 玻璃卡片外壳。
 * 抽取共享的玻璃形态样式(el-card :deep(...) + 背景 / 圆角 / 阴影),
 * 让父组件 DashboardView 只管布局,卡片自身管样式。
 *
 * @slot 默认 slot:卡片 body 内容
 */
</script>

<style scoped>
.dashboard-view__card {
  background: rgba(255, 255, 255, 0.5) !important;
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 16px;
  box-shadow:
    0 12px 32px rgba(183, 50, 92, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.dashboard-view__card:hover {
  transform: translateY(-2px);
  box-shadow:
    0 18px 42px rgba(183, 50, 92, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

.dashboard-view__card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(183, 50, 92, 0.08);
}

.dashboard-view__card :deep(.el-card__body) {
  padding: 20px;
}
</style>