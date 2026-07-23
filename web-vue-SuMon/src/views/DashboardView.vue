<template>
  <div class="dashboard-view">
    <DashboardHero
      :username="auth.user?.username"
      :role="auth.user?.role"
      :review-status="auth.user?.reviewStatus"
      :role-label="auth.user?.role ? userRoleLabel(auth.user.role) : ''"
      :refreshing="refreshing"
      :logging-out="loggingOut"
      @refresh="refresh"
      @logout="handleLogout"
    />

    <el-row
      :gutter="20"
      class="dashboard-view__cards"
    >
      <el-col
        :xs="24"
        :sm="12"
        :md="8"
      >
        <DashboardProbeCard
          title="健康检查"
          :ok="health.ok"
          :detail="health.detail"
          hint="后端应用标识"
          :loading="loading.health"
          :pulse="true"
          ok-label="UP"
        />
      </el-col>

      <el-col
        :xs="24"
        :sm="12"
        :md="8"
      >
        <DashboardProbeCard
          title="就绪检查"
          :ok="ready.ok"
          :detail="ready.detail"
          hint="数据库健康状态"
          :loading="loading.ready"
          ok-label="READY"
        />
      </el-col>

      <el-col
        :xs="24"
        :sm="12"
        :md="8"
      >
        <DashboardServersCard
          :count="servers.count"
          :displayed-total="displayedTotal"
          :loading="loading.servers"
        />
      </el-col>
    </el-row>

    <el-row
      v-if="auth.isAdmin"
      :gutter="20"
      class="dashboard-view__cards"
    >
      <el-col
        :xs="24"
        :md="12"
      >
        <DashboardAdminCard
          :pending-count="pending.count"
          @review="goAdminUsers"
          @refresh="refresh"
        />
      </el-col>

      <el-col
        :xs="24"
        :md="12"
      >
        <DashboardSshCard />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
/**
 * Dashboard 视图。
 *
 * 子组件:
 * - DashboardHero: 顶部欢迎英雄条
 * - DashboardProbeCard: 健康 / 就绪检查(共用)
 * - DashboardServersCard: 服务器总数 + spark line
 * - DashboardAdminCard: 管理员快速入口(admin-only)
 * - DashboardSshCard: SSH 测试历史占位
 *
 * 主组件只保留数据加载逻辑 + 子组件编排。
 */

import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ApiBusinessError } from '@/api/client'
import { getHealth, getReady } from '@/api/system'
import { listServers } from '@/api/server'
import { useAuthStore } from '@/stores/auth'
import { ErrorCode } from '@/types/error-code'
import { animateCounter } from '@/utils/animate'
import { userRoleLabel } from '@/utils/format'
import DashboardHero from '@/components/DashboardHero.vue'
import DashboardProbeCard from '@/components/DashboardProbeCard.vue'
import DashboardServersCard from '@/components/DashboardServersCard.vue'
import DashboardAdminCard from '@/components/DashboardAdminCard.vue'
import DashboardSshCard from '@/components/DashboardSshCard.vue'

const router = useRouter()
const auth = useAuthStore()

interface ProbeResult {
  ok: boolean
  detail: string
}
interface CountedProbe extends ProbeResult {
  count: number
}

const loading = reactive({ health: true, ready: true, servers: true, pending: true })
const refreshing = ref(false)
const loggingOut = ref(false)
const health = ref<ProbeResult>({ ok: false, detail: '' })
const ready = ref<ProbeResult>({ ok: false, detail: '' })
const servers = ref<CountedProbe>({ ok: false, detail: '', count: 0 })
const pending = ref<CountedProbe>({ ok: false, detail: '', count: 0 })

/**
 * 服务器总数的滚动显示值。servers.count 是后端返回的真值,
 * displayedTotal 是当前帧呈现给用户的浮点值,二者差异由 animateCounter 驱动收敛。
 */
const displayedTotal = ref(0)

/**
 * 滚动动画的取消函数,组件卸载或新一轮动画启动时调用。
 */
let counterCancel: (() => void) | null = null

/**
 * 将 API 业务异常映射为探针结果,避免 UI 层吞错。
 */
function mapErrorToProbe(error: unknown, fallback: string): ProbeResult {
  if (error instanceof ApiBusinessError) {
    if (error.code === ErrorCode.UNAUTHORIZED) {
      return { ok: false, detail: '需要登录' }
    }
    if (error.code === ErrorCode.FORBIDDEN) {
      return { ok: false, detail: '无权访问' }
    }
    return { ok: false, detail: error.message || fallback }
  }
  return { ok: false, detail: fallback }
}

async function probeHealth(): Promise<void> {
  try {
    const response = await getHealth()
    health.value = {
      ok: response.code === ErrorCode.SUCCESS,
      detail: response.data?.application ?? '-'
    }
  } catch (error) {
    health.value = mapErrorToProbe(error, '后端不可达')
  } finally {
    loading.health = false
  }
}

async function probeReady(): Promise<void> {
  try {
    const response = await getReady()
    ready.value = {
      ok: response.code === ErrorCode.SUCCESS,
      detail: response.data?.database ?? '-'
    }
  } catch (error) {
    ready.value = mapErrorToProbe(error, '后端不可达')
  } finally {
    loading.ready = false
  }
}

async function probeServers(): Promise<void> {
  try {
    const response = await listServers({ page: 1, page_size: 1 })
    const target = response.data?.total ?? 0
    servers.value = {
      ok: response.code === ErrorCode.SUCCESS,
      detail: '见 /api/servers 列表',
      count: target
    }
    if (servers.value.ok) {
      runCounter(target)
    } else {
      displayedTotal.value = 0
    }
  } catch (error) {
    servers.value = { ...mapErrorToProbe(error, '需要登录'), count: 0 }
    displayedTotal.value = 0
  } finally {
    loading.servers = false
  }
}

/**
 * 启动/重启动画:从当前显示值滑到新目标值,easeOutCubic,1.2s。
 */
function runCounter(target: number): void {
  if (counterCancel) {
    counterCancel()
    counterCancel = null
  }
  if (target <= 0) {
    displayedTotal.value = 0
    return
  }
  counterCancel = animateCounter(
    displayedTotal.value,
    target,
    1200,
    (v) => {
      displayedTotal.value = Math.round(v)
    },
    () => {
      counterCancel = null
    }
  )
}

/**
 * 拉取待审核用户列表,仅 admin 调用。失败时降级展示。
 */
async function probePendingCount(): Promise<void> {
  if (!auth.isAdmin) {
    pending.value = { ok: false, detail: '非管理员', count: 0 }
    loading.pending = false
    return
  }
  try {
    const { listPendingUsers } = await import('@/api/admin')
    const response = await listPendingUsers()
    pending.value = {
      ok: response.code === ErrorCode.SUCCESS,
      detail: '当前待审核用户数',
      count: Array.isArray(response.data) ? response.data.length : 0
    }
  } catch (error) {
    pending.value = { ...mapErrorToProbe(error, '加载失败'), count: 0 }
  } finally {
    loading.pending = false
  }
}

async function refresh(): Promise<void> {
  if (refreshing.value) {
    return
  }
  refreshing.value = true
  loading.health = true
  loading.ready = true
  loading.servers = true
  loading.pending = true
  await Promise.all([probeHealth(), probeReady(), probeServers(), probePendingCount()])
  refreshing.value = false
}

function goAdminUsers(): void {
  void router.push({ name: 'admin-users' })
}

/**
 * 调用 auth store 退出登录,无论后端是否成功都清空本地会话,
 * 然后跳到登录页并给用户一个 toast 反馈。
 */
async function handleLogout(): Promise<void> {
  if (loggingOut.value) {
    return
  }
  loggingOut.value = true
  try {
    await auth.logout()
    ElMessage.success('已退出登录')
    await router.push({ name: 'login' })
  } finally {
    loggingOut.value = false
  }
}

onMounted(() => {
  void refresh()
})

onUnmounted(() => {
  if (counterCancel) {
    counterCancel()
    counterCancel = null
  }
})
</script>

<style scoped>
.dashboard-view {
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-view__cards {
  margin-bottom: 20px;
}

.dashboard-view__cards:last-child {
  margin-bottom: 0;
}

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

.dashboard-view__card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dashboard-view__card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #2a1626;
}

.dashboard-view__pulse {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #ff5b8a;
  border-radius: 50%;
  box-shadow: 0 0 0 0 rgba(255, 91, 138, 0.6);
  animation: dashboard-pulse 1.6s ease-out infinite;
}

@keyframes dashboard-pulse {
  0%   { box-shadow: 0 0 0 0 rgba(255, 91, 138, 0.6); }
  70%  { box-shadow: 0 0 0 10px rgba(255, 91, 138, 0); }
  100% { box-shadow: 0 0 0 0 rgba(255, 91, 138, 0); }
}

.dashboard-view__spark-wrap {
  margin-top: 12px;
  padding: 8px 4px 4px;
  border-top: 1px dashed rgba(183, 50, 92, 0.12);
}

.dashboard-view__spark {
  display: block;
  width: 100%;
  height: 32px;
}

.dashboard-view__spark-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
  font-size: 11px;
  color: #8a5872;
  letter-spacing: 0.5px;
}

.dashboard-view__spark-label {
  font-style: italic;
}

.dashboard-view__spark-delta {
  font-weight: 700;
  color: #2eb872;
  font-variant-numeric: tabular-nums;
}

.dashboard-view__ssh-placeholder {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.dashboard-view__ssh-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.dashboard-view__ssh-label {
  color: #8a5872;
  letter-spacing: 0.5px;
}

.dashboard-view__ssh-value {
  color: #2a1626;
  font-weight: 600;
}

.dashboard-view__ssh-hint {
  margin-top: 8px;
  padding: 8px 10px;
  font-size: 11px;
  color: #6d3b54;
  background: rgba(245, 185, 66, 0.08);
  border: 1px dashed rgba(245, 185, 66, 0.35);
  border-radius: 8px;
  line-height: 1.5;
  letter-spacing: 0.3px;
}

.dashboard-view__badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #fff;
  white-space: nowrap;
}

.dashboard-view__badge--ok {
  background: linear-gradient(135deg, #66e6a8 0%, #2eb872 100%);
  box-shadow: 0 2px 8px rgba(46, 184, 114, 0.35);
}

.dashboard-view__badge--down {
  background: linear-gradient(135deg, #f86c6c 0%, #b7325c 100%);
  box-shadow: 0 2px 8px rgba(183, 50, 92, 0.35);
}

.dashboard-view__badge--info {
  background: linear-gradient(135deg, #ff7aa3 0%, #b7325c 100%);
  box-shadow: 0 2px 8px rgba(255, 91, 138, 0.3);
}

.dashboard-view__card-value {
  font-size: 20px;
  font-weight: 700;
  color: #2a1626;
  line-height: 1.4;
  word-break: break-all;
}

.dashboard-view__card-value--accent {
  font-size: 36px;
  background: linear-gradient(135deg, #b7325c 0%, #f5b942 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 1px;
  font-variant-numeric: tabular-nums;
}

.dashboard-view__card-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #8a5872;
  letter-spacing: 0.5px;
}

.dashboard-view__card-meta {
  font-size: 13px;
  color: #6d3b54;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.dashboard-view__admin-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.dashboard-view__admin-cta {
  background: linear-gradient(135deg, #ff5b8a 0%, #b7325c 100%) !important;
  border: none !important;
  box-shadow: 0 6px 14px rgba(255, 91, 138, 0.3) !important;
}

.dashboard-view__admin-cta:hover {
  background: linear-gradient(135deg, #ff7aa3 0%, #c8426f 100%) !important;
  box-shadow: 0 10px 20px rgba(255, 91, 138, 0.45) !important;
}
</style>