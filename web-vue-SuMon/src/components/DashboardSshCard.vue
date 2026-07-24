<template>
  <el-card
    class="dashboard-view__card dashboard-view__card--glass"
    shadow="never"
  >
    <template #header>
      <div class="dashboard-view__card-header">
        <div class="dashboard-view__card-title">
          <TushanFoxMark
            :size="28"
            alt="涂山苏苏·SSH 测试"
          />
          上次 SSH 测试结果
        </div>
        <el-tag
          type="info"
          size="small"
          effect="plain"
        >
          占位
        </el-tag>
      </div>
    </template>
    <div class="dashboard-view__ssh-placeholder">
      <div class="dashboard-view__ssh-line">
        <span class="dashboard-view__ssh-label">最近一次</span>
        <span class="dashboard-view__ssh-value">暂无记录</span>
      </div>
      <div class="dashboard-view__ssh-line">
        <span class="dashboard-view__ssh-label">目标服务器</span>
        <span class="dashboard-view__ssh-value">—</span>
      </div>
      <div class="dashboard-view__ssh-line">
        <span class="dashboard-view__ssh-label">耗时</span>
        <span class="dashboard-view__ssh-value">—</span>
      </div>
      <div class="dashboard-view__ssh-hint">
        等待后端 /api/servers/{id}/ssh/test 历史接口;MVP-1 当前仅实现单次测试,历史接入时自动填充。
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import TushanFoxMark from '@/components/TushanFoxMark.vue'

/**
 * Dashboard SSH 测试历史占位卡。
 * 等后端 /api/servers/{id}/ssh/test 历史接口接入后填充真实数据。
 */
</script>