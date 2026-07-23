<template>
  <el-card
    class="dashboard-view__card dashboard-view__card--glass"
    shadow="never"
  >
    <template #header>
      <div class="dashboard-view__card-header">
        <div class="dashboard-view__card-title">
          管理员快速入口
        </div>
        <span class="dashboard-view__card-meta">
          待审核用户:
          <el-tag
            :type="pendingCount > 0 ? 'warning' : 'info'"
            size="small"
            effect="dark"
          >
            {{ pendingCount }}
          </el-tag>
        </span>
      </div>
    </template>
    <div class="dashboard-view__admin-actions">
      <el-button
        type="primary"
        :disabled="pendingCount === 0"
        class="dashboard-view__admin-cta"
        @click="emit('review')"
      >
        前往审核
      </el-button>
      <el-button @click="emit('refresh')">
        重新加载
      </el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
/**
 * Dashboard 管理员快速入口卡(admin-only)。
 *
 * @prop pendingCount 待审核用户数
 * @event review 点击"前往审核"
 * @event refresh 点击"重新加载"
 */

defineProps<{
  pendingCount: number
}>()

const emit = defineEmits<{
  (e: 'review'): void
  (e: 'refresh'): void
}>()
</script>