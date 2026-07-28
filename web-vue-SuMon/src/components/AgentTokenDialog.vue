<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="560px"
    :close-on-click-modal="false"
    :show-close="!loading"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
    @close="onClose"
  >
    <div class="agent-token-dialog__intro">
      <p>{{ introText }}</p>
    </div>

    <el-alert
      v-if="plaintextToken !== null"
      type="success"
      :closable="false"
      show-icon
      class="agent-token-dialog__token-box"
    >
      <template #title>
        <div class="agent-token-dialog__token-title">
          <span class="agent-token-dialog__token-label">明文 Agent Token(仅显示一次)</span>
          <el-button
            size="small"
            type="primary"
            plain
            :disabled="copied"
            @click="copyToken"
          >
            {{ copied ? '已复制' : '复制' }}
          </el-button>
        </div>
      </template>
      <code class="agent-token-dialog__token-code">{{ plaintextToken }}</code>
      <div class="agent-token-dialog__token-meta">
        创建时间:{{ formatDateTime(plaintextCreatedAt) }}
      </div>
    </el-alert>

    <el-alert
      v-if="resultMessage"
      :title="resultMessage"
      :type="resultType"
      :closable="false"
      show-icon
      class="agent-token-dialog__result"
    />

    <template #footer>
      <el-button
        :disabled="loading"
        @click="onCancel"
      >
        关闭
      </el-button>
      <el-button
        type="primary"
        :loading="loading"
        @click="onConfirm"
      >
        {{ confirmText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
/**
 * Agent Token 管理对话框(MVP-2 / B-038)。
 *
 * 模式(mode prop):
 *   - 'register' 首次生成
 *   - 'rotate'   显式轮换(旧 Token 立即失效)
 *
 * 设计要点:
 *   - 明文 Token 仅在响应中返回一次,dialog 内 el-alert 一次性展示 + 复制按钮
 *   - 明文 Token 不会写入 Pinia store / localStorage / 日志
 *   - 关闭 dialog 时清空 plaintextToken,避免组件复用泄漏
 *   - 失败抛 ApiBusinessError 由 axios 拦截器统一 toast,dialog 保留打开供重试
 *
 * 注:`revoke` 模式不需要 dialog 入口,直接由 ServerDetailView 用 ElPopconfirm 二次
 * 确认即可;此处只覆盖 register / rotate 两个返回明文的操作。
 */

import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ApiBusinessError } from '@/api/client'
import { registerAgentToken, rotateAgentToken } from '@/api/agent-token'
import type { AgentToken } from '@/types/api'
import { ErrorCode } from '@/types/error-code'
import { formatDateTime } from '@/utils/format'

type DialogMode = 'register' | 'rotate'

const props = defineProps<{
  /** v-model 绑定 */
  modelValue: boolean
  /** 模式:首次生成 / 显式轮换 */
  mode: DialogMode
  /** 目标服务器 ID,必填 */
  serverId: number
  /** 服务器名称,只用于文案 */
  serverName?: string
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'success', payload: AgentToken): void
}>()

const loading = ref(false)
const plaintextToken = ref<string | null>(null)
const plaintextCreatedAt = ref<string | null>(null)
const copied = ref(false)
const resultMessage = ref<string | null>(null)
const resultType = ref<'success' | 'error' | 'warning' | 'info'>('info')

const title = computed(() => {
  const serverLabel = props.serverName ? ` #${props.serverId} · ${props.serverName}` : ` #${props.serverId}`
  return props.mode === 'rotate' ? `轮换 Agent Token${serverLabel}` : `生成 Agent Token${serverLabel}`
})

const introText = computed(() => {
  if (props.mode === 'rotate') {
    return '轮换将使旧 Agent Token 立即失效,新 Token 仅在本次响应中返回一次。请在关闭对话框前复制并妥善保存。'
  }
  return '首次生成 Agent Token 用于绑定服务器和采集 Agent;明文仅在本次响应中返回一次。请在关闭对话框前复制并妥善保存。'
})

const confirmText = computed(() => (props.mode === 'rotate' ? '立即轮换' : '立即生成'))

/**
 * 关闭 dialog 时清空一次性明文和结果提示,避免下一次打开残留。
 */
function resetState(): void {
  plaintextToken.value = null
  plaintextCreatedAt.value = null
  copied.value = false
  resultMessage.value = null
  resultType.value = 'info'
}

/**
 * 监听 modelValue 切换时清空状态;避免切换不同服务器实例时残留旧明文。
 */
watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      resetState()
    }
  }
)

async function onConfirm(): Promise<void> {
  if (props.serverId <= 0) {
    ElMessage.error('目标服务器 ID 非法')
    return
  }
  loading.value = true
  resultMessage.value = null
  try {
    const response = props.mode === 'rotate'
      ? await rotateAgentToken(props.serverId)
      : await registerAgentToken(props.serverId)
    plaintextToken.value = response.data.agent_token
    plaintextCreatedAt.value = response.data.created_at
    resultMessage.value = props.mode === 'rotate'
      ? '轮换成功。旧 Token 立即失效。'
      : '生成成功。'
    resultType.value = 'success'
    ElMessage.success(resultMessage.value)
    emit('success', response.data)
  } catch (error) {
    if (error instanceof ApiBusinessError) {
      switch (error.code) {
        case ErrorCode.FORBIDDEN:
          resultMessage.value = '无权限:仅管理员可管理 Agent Token'
          break
        case ErrorCode.UNAUTHORIZED:
          resultMessage.value = '未登录或登录已过期'
          break
        case ErrorCode.RESOURCE_CONFLICT:
          resultMessage.value = '当前 Agent Token 状态冲突,请刷新后重试'
          break
        case ErrorCode.RESOURCE_NOT_FOUND:
          resultMessage.value = '服务器不存在或已被删除'
          break
        default:
          resultMessage.value = error.message || '操作失败'
      }
      resultType.value = 'error'
    } else {
      resultMessage.value = '网络异常,请稍后重试'
      resultType.value = 'error'
    }
  } finally {
    loading.value = false
  }
}

async function copyToken(): Promise<void> {
  const value = plaintextToken.value
  if (value === null) {
    return
  }
  try {
    if (typeof navigator !== 'undefined' && navigator.clipboard !== undefined) {
      await navigator.clipboard.writeText(value)
    } else {
      // 旧浏览器 fallback:用临时 textarea + document.execCommand('copy')
      const textarea = document.createElement('textarea')
      textarea.value = value
      textarea.setAttribute('readonly', 'readonly')
      textarea.style.position = 'absolute'
      textarea.style.left = '-9999px'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    copied.value = true
    ElMessage.success('Token 已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败,请手动选择复制')
  }
}

function onCancel(): void {
  if (plaintextToken.value !== null) {
    ElMessage.warning('请确认已保存 Token,关闭后无法再次查看')
  }
  emit('update:modelValue', false)
}

function onClose(): void {
  resetState()
}
</script>

<style scoped>
.agent-token-dialog__intro {
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.agent-token-dialog__intro p {
  margin: 0;
  line-height: 1.6;
}

.agent-token-dialog__token-box {
  margin-bottom: 12px;
}

.agent-token-dialog__token-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.agent-token-dialog__token-label {
  font-weight: 600;
}

.agent-token-dialog__token-code {
  display: block;
  margin-top: 8px;
  padding: 10px 12px;
  font-family: 'JetBrains Mono', 'Consolas', monospace;
  font-size: 13px;
  word-break: break-all;
  background-color: var(--el-fill-color-light);
  border-radius: 4px;
  border: 1px solid var(--el-border-color-lighter);
}

.agent-token-dialog__token-meta {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.agent-token-dialog__result {
  margin-bottom: 12px;
}
</style>