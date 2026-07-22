<template>
  <div class="dashboard-view">
    <header class="dashboard-view__hero">
      <div class="dashboard-view__hero-text">
        <h2 class="dashboard-view__hero-title">
          苏苏欢迎你回来,
          <strong class="dashboard-view__hero-name">
            {{ auth.user?.username ?? '小道士' }}
          </strong>
          !
        </h2>
        <p class="dashboard-view__hero-sub">
          涂山小队的服务器今日一切安好~
        </p>
      </div>
      <div class="dashboard-view__hero-meta">
        <el-tag
          v-if="auth.user"
          :type="auth.user.role === 'admin' ? 'danger' : 'info'"
          effect="dark"
          size="default"
        >
          {{ userRoleLabel(auth.user.role) }}
        </el-tag>
        <el-tag
          v-if="auth.user"
          :type="reviewStatusTagType(auth.user.reviewStatus)"
          effect="plain"
          size="default"
        >
          {{ reviewStatusLabel(auth.user.reviewStatus) }}
        </el-tag>
        <el-button
          circle
          :loading="refreshing"
          class="dashboard-view__refresh-btn"
          :aria-label="'刷新仪表盘'"
          @click="refresh"
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
          class="dashboard-view__logout-btn"
          aria-label="退出当前账号"
          @click="handleLogout"
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

    <el-row
      :gutter="20"
      class="dashboard-view__cards"
    >
      <el-col
        :xs="24"
        :sm="12"
        :md="8"
      >
        <el-card
          class="dashboard-view__card dashboard-view__card--glass"
          shadow="never"
        >
          <template #header>
            <div class="dashboard-view__card-header">
              <div class="dashboard-view__card-title">
                <TushanFoxMark
                  :size="32"
                  alt="涂山苏苏·健康"
                />
                <span
                  v-if="health.ok"
                  class="dashboard-view__pulse"
                  aria-hidden="true"
                />
                健康检查
              </div>
              <span
                class="dashboard-view__badge"
                :class="`dashboard-view__badge--${health.ok ? 'ok' : 'down'}`"
              >
                {{ health.ok ? 'UP' : 'DOWN' }}
              </span>
            </div>
          </template>
          <el-skeleton
            v-if="loading.health"
            :rows="2"
            animated
          />
          <template v-else>
            <div class="dashboard-view__card-value">
              {{ health.ok ? health.detail : '后端不可达' }}
            </div>
            <div class="dashboard-view__card-hint">
              后端应用标识
            </div>
          </template>
        </el-card>
      </el-col>

      <el-col
        :xs="24"
        :sm="12"
        :md="8"
      >
        <el-card
          class="dashboard-view__card dashboard-view__card--glass"
          shadow="never"
        >
          <template #header>
            <div class="dashboard-view__card-header">
              <div class="dashboard-view__card-title">
                <TushanFoxMark
                  :size="32"
                  alt="涂山苏苏·就绪"
                />
                就绪检查
              </div>
              <span
                class="dashboard-view__badge"
                :class="`dashboard-view__badge--${ready.ok ? 'ok' : 'down'}`"
              >
                {{ ready.ok ? 'READY' : 'NOT READY' }}
              </span>
            </div>
          </template>
          <el-skeleton
            v-if="loading.ready"
            :rows="2"
            animated
          />
          <template v-else>
            <div class="dashboard-view__card-value">
              数据库：{{ ready.ok ? ready.detail : '后端不可达' }}
            </div>
            <div class="dashboard-view__card-hint">
              数据库健康状态
            </div>
          </template>
        </el-card>
      </el-col>

      <el-col
        :xs="24"
        :sm="12"
        :md="8"
      >
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
                {{ servers.ok ? servers.count : 'N/A' }}
              </span>
            </div>
          </template>
          <el-skeleton
            v-if="loading.servers"
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
                  :type="pending.ok && pending.count > 0 ? 'warning' : 'info'"
                  size="small"
                  effect="dark"
                >
                  {{ pending.ok ? pending.count : '-' }}
                </el-tag>
              </span>
            </div>
          </template>
          <div class="dashboard-view__admin-actions">
            <el-button
              type="primary"
              :disabled="!pending.ok || pending.count === 0"
              class="dashboard-view__admin-cta"
              @click="goAdminUsers"
            >
              前往审核
            </el-button>
            <el-button @click="refresh">
              重新加载
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col
        :xs="24"
        :md="12"
      >
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
                :type="sshMock.lastStatus === 'success' ? 'success' : 'info'"
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
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiBusinessError } from '@/api/client'
import { getHealth } from '@/api/system'
import { getReady } from '@/api/system'
import { listServers } from '@/api/server'
import { useAuthStore } from '@/stores/auth'
import { ErrorCode } from '@/types/error-code'
import { animateCounter } from '@/utils/animate'
import { reviewStatusLabel, reviewStatusTagType, userRoleLabel } from '@/utils/format'
import TushanFoxMark from '@/components/TushanFoxMark.vue'

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
 * spark line 用的服务器总数历史值(模拟数据,7 个点)。
 * 后端 `/api/metrics/history` 接入后改为响应式 ref + 实际接口数据。
 * 这里以 displayedTotal 为终点构造"近期缓步上升"的曲线,不假装为真实数据。
 */
const sparkData = computed<number[]>(() => {
  const target = Math.max(displayedTotal.value, 0)
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
 * spark line 的折线点字符串(viewBox 0..100 宽,0..30 高)。
 * 计算时考虑数值归一化,避免历史跨度盖住当前值。
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
 * spark line 下方的填充区域(折线到 30 高度的闭合)。
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

/**
 * 占位:等待后端 SSH test history 接口。
 * 接入后这里改为从 /api/servers/{id}/ssh/history 获取最近一次结果。
 */
const sshMock = {
  hasRecord: false,
  lastStatus: 'idle' as 'success' | 'failed' | 'idle'
}

/**
 * 滚动动画的取消函数,组件卸载或新一轮动画启动时调用,
 * 防止旧动画在异步竞态下继续 setState 已被销毁的 ref。
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
 * 若目标 <= 0 则直接归零;若与当前值相等则跳过动画。
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
 * 拉取待审核用户列表,仅 admin 调用。失败时降级展示,UI 不报错弹窗。
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

/**
 * dashboard 内的"退出登录"按钮加载状态,防止重复点击。
 */
const loggingOut = ref(false)

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

/* ---------- 欢迎英雄条 ---------- */
.dashboard-view__hero {
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

.dashboard-view__hero::before {
  content: '';
  position: absolute;
  top: -40%;
  right: -10%;
  width: 240px;
  height: 240px;
  background: radial-gradient(circle, rgba(255, 91, 138, 0.18) 0%, transparent 70%);
  pointer-events: none;
}

.dashboard-view__hero-text {
  flex: 1;
  min-width: 0;
}

.dashboard-view__hero-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #2a1626;
  letter-spacing: 1px;
  line-height: 1.4;
}

.dashboard-view__hero-name {
  background: linear-gradient(135deg, #b7325c 0%, #f5b942 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  font-weight: 800;
  padding: 0 2px;
}

.dashboard-view__hero-sub {
  margin: 8px 0 0;
  font-size: 13px;
  color: #6d3b54;
  letter-spacing: 0.5px;
  font-style: italic;
}

.dashboard-view__hero-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

/* ---------- 圆形刷新按钮 ---------- */
.dashboard-view__refresh-btn {
  width: 40px;
  height: 40px;
  padding: 0;
  background: linear-gradient(135deg, #ff5b8a 0%, #b7325c 100%);
  border: none;
  color: #fff;
  box-shadow: 0 6px 14px rgba(255, 91, 138, 0.3);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.dashboard-view__refresh-btn:hover:not(.is-loading) {
  background: linear-gradient(135deg, #ff7aa3 0%, #c8426f 100%);
  box-shadow: 0 10px 20px rgba(255, 91, 138, 0.45);
}

.dashboard-view__refresh-btn svg {
  width: 20px;
  height: 20px;
  transition: transform 0.8s ease-in-out;
}

.dashboard-view__refresh-btn:hover:not(.is-loading) svg {
  transform: rotate(360deg);
}

/* ---------- 退出登录按钮 ---------- */
.dashboard-view__logout-btn {
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

.dashboard-view__logout-btn:hover {
  color: #fff;
  background: linear-gradient(135deg, #ff5b8a 0%, #b7325c 100%);
  border-color: #b7325c;
  box-shadow: 0 6px 14px rgba(255, 91, 138, 0.35);
  transform: translateY(-1px);
}

.dashboard-view__logout-btn svg {
  width: 18px;
  height: 18px;
  transition: transform 0.3s ease;
}

.dashboard-view__logout-btn:hover svg {
  transform: translateX(2px);
}

/* ---------- 卡片网格 ---------- */
.dashboard-view__cards {
  margin-bottom: 20px;
}

.dashboard-view__cards:last-child {
  margin-bottom: 0;
}

/* ---------- 玻璃形态卡片 ---------- */
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

/* ---------- 健康脉冲点 ---------- */
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

/* ---------- spark line 区域 ---------- */
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

/* ---------- SSH 测试占位卡 ---------- */
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

/* ---------- 自定义徽标(替代 el-tag 单色) ---------- */
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

/* ---------- 卡片主数值 ---------- */
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

/* ---------- 管理员卡片操作 ---------- */
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

/* ---------- 响应式 ---------- */
@media (max-width: 720px) {
  .dashboard-view__hero {
    flex-direction: column;
    align-items: stretch;
  }

  .dashboard-view__hero-meta {
    justify-content: flex-end;
  }

  .dashboard-view__hero-title {
    font-size: 20px;
  }
}
</style>