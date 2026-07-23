<template>
  <header class="dashboard-hero">
    <div class="dashboard-hero__text">
      <h2 class="dashboard-hero__title">
        苏苏欢迎你回来,
        <strong class="dashboard-hero__name">
          {{ username ?? '小道士' }}
        </strong>
        !
      </h2>
      <p class="dashboard-hero__sub">
        涂山小队的服务器今日一切安好~
      </p>
    </div>
    <div class="dashboard-hero__meta">
      <el-tag
        v-if="role"
        :type="role === 'admin' ? 'danger' : 'info'"
        effect="dark"
        size="default"
      >
        {{ roleLabel }}
      </el-tag>
      <el-tag
        v-if="reviewStatus"
        :type="reviewStatusTagType(reviewStatus)"
        effect="plain"
        size="default"
      >
        {{ reviewStatusLabel(reviewStatus) }}
      </el-tag>
      <el-button
        circle
        :loading="refreshing"
        class="dashboard-hero__refresh-btn"
        :aria-label="'刷新仪表盘'"
        @click="emit('refresh')"
      >
        <svg
          viewBox="0 0 24 24"
          xmlns="http://www.w3.org/2000/svg"
          aria-hidden="true"
        >
          <path
            d="M4 12 A8 8 0 0 1 18 7"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
          />
          <path
            d="M18 4 L18 8 L14 8"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <path
            d="M20 12 A8 8 0 0 1 6 17"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
          />
          <path
            d="M6 20 L6 16 L10 16"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </el-button>
      <el-button
        :loading="loggingOut"
        class="dashboard-hero__logout-btn"
        aria-label="退出当前账号"
        @click="emit('logout')"
      >
        <svg
          viewBox="0 0 24 24"
          xmlns="http://www.w3.org/2000/svg"
          aria-hidden="true"
        >
          <path
            d="M14 4 L14 6 L18 6 L18 18 L14 18 L14 20 L20 20 L20 4 Z"
            fill="currentColor"
            opacity="0.85"
          />
          <path
            d="M3 12 L13 12"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <path
            d="M9 8 L13 12 L9 16"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        <span>退出登录</span>
      </el-button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { reviewStatusLabel, reviewStatusTagType } from '@/utils/format'
import type { ReviewStatus, UserRole } from '@/types/api'

/**
 * Dashboard 顶部欢迎英雄条。
 *
 * @prop username 当前用户名
 * @prop role 用户角色(admin / user)
 * @prop reviewStatus 审核状态
 * @prop roleLabel 角色中文标签(由父组件传入,避免重复调用)
 * @prop refreshing 刷新中状态
 * @prop loggingOut 退出登录中状态
 * @event refresh 点击刷新按钮
 * @event logout 点击退出登录按钮
 */

defineProps<{
  username?: string
  role?: UserRole
  reviewStatus?: ReviewStatus
  roleLabel: string
  refreshing: boolean
  loggingOut: boolean
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
  (e: 'logout'): void
}>()
</script>

<style scoped>
.dashboard-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 28px;
  margin-bottom: 24px;
  background:
    linear-gradient(135deg, rgba(255, 232, 239, 0.55) 0%, rgba(255, 215, 220, 0.35) 100%),
    rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 18px;
  backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
  box-shadow:
    0 12px 32px rgba(183, 50, 92, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
  position: relative;
  overflow: hidden;
}

.dashboard-hero::before {
  content: '';
  position: absolute;
  top: -40%;
  right: -10%;
  width: 240px;
  height: 240px;
  background: radial-gradient(circle, rgba(255, 91, 138, 0.18) 0%, transparent 70%);
  pointer-events: none;
}

.dashboard-hero__text {
  flex: 1;
  min-width: 0;
}

.dashboard-hero__title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #2a1626;
  letter-spacing: 1px;
  line-height: 1.4;
}

.dashboard-hero__name {
  background: linear-gradient(135deg, #b7325c 0%, #f5b942 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  font-weight: 800;
  padding: 0 2px;
}

.dashboard-hero__sub {
  margin: 8px 0 0;
  font-size: 13px;
  color: #6d3b54;
  letter-spacing: 0.5px;
  font-style: italic;
}

.dashboard-hero__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.dashboard-hero__refresh-btn {
  width: 40px;
  height: 40px;
  padding: 0;
  background: linear-gradient(135deg, #ff5b8a 0%, #b7325c 100%);
  border: none;
  color: #fff;
  box-shadow: 0 6px 14px rgba(255, 91, 138, 0.3);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.dashboard-hero__refresh-btn:hover:not(.is-loading) {
  background: linear-gradient(135deg, #ff7aa3 0%, #c8426f 100%);
  box-shadow: 0 10px 20px rgba(255, 91, 138, 0.45);
}

.dashboard-hero__refresh-btn svg {
  width: 20px;
  height: 20px;
  transition: transform 0.8s ease-in-out;
}

.dashboard-hero__refresh-btn:hover:not(.is-loading) svg {
  transform: rotate(360deg);
}

.dashboard-hero__logout-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 14px;
  height: 40px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #b7325c;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(183, 50, 92, 0.3);
  border-radius: 20px;
  backdrop-filter: blur(8px);
  transition: all 0.2s ease;
}

.dashboard-hero__logout-btn:hover {
  color: #fff;
  background: linear-gradient(135deg, #ff5b8a 0%, #b7325c 100%);
  border-color: #b7325c;
  box-shadow: 0 6px 14px rgba(183, 50, 92, 0.35);
  transform: translateY(-1px);
}

.dashboard-hero__logout-btn svg {
  width: 18px;
  height: 18px;
  transition: transform 0.3s ease;
}

.dashboard-hero__logout-btn:hover svg {
  transform: translateX(2px);
}

@media (max-width: 720px) {
  .dashboard-hero {
    flex-direction: column;
    align-items: stretch;
  }

  .dashboard-hero__meta {
    justify-content: flex-end;
  }

  .dashboard-hero__title {
    font-size: 20px;
  }
}
</style>