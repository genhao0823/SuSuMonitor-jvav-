import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  createAlertRule,
  deleteAlertRule,
  isAlertPush,
  listAlertRecords,
  listAlertRules,
  markAlertRecordAsRead,
  updateAlertRule
} from '@/api/alert'
import type {
  AlertPushPayload,
  AlertRecord,
  AlertRecordQuery,
  AlertRule,
  CreateAlertRuleRequest,
  UpdateAlertRuleRequest
} from '@/types/api'

/**
 * 告警 Pinia store(MVP-6)。
 *
 * 持久化策略:**不启用** `pinia-plugin-persistedstate`。
 * 原因:告警数据时效性强,持久化过期未读数 / 过期规则既无价值又会误导用户;
 * REST 始终为真源,任何刷新或重连都应重新拉取。
 *
 * HTTP 错误与 WS 错误分开保存,避免 WS 断线覆盖 REST 错误。
 */

/** 告警记录查询的默认页大小,与后端 OpenAPI 默认对齐。 */
const DEFAULT_PAGE_SIZE = 20

export const useAlertsStore = defineStore('alerts', () => {
  // ===== 规则 =====
  const rules = ref<AlertRule[]>([])
  const rulesLoading = ref(false)
  const rulesError = ref<string | null>(null)

  // ===== 记录 =====
  const records = ref<AlertRecord[]>([])
  const recordsTotal = ref(0)
  const recordsQuery = ref<AlertRecordQuery>({})
  const recordsLoading = ref(false)
  const recordsError = ref<string | null>(null)

  // ===== WS 实时推送 =====
  /** 顶部轻提示条用的待刷新计数;每次 alert.push +1,用户点"刷新"后清零。 */
  const pendingPushCount = ref(0)
  const wsConnected = ref(false)
  const wsError = ref<string | null>(null)

  /**
   * 加载全部告警规则。
   * 失败时 `rulesError` 被设置,`rules` 保留上一次成功结果,不主动清空。
   */
  async function loadRules(): Promise<void> {
    rulesLoading.value = true
    rulesError.value = null
    try {
      const response = await listAlertRules()
      rules.value = response.data
    } catch (reason) {
      rulesError.value = reason instanceof Error ? reason.message : '规则加载失败'
    } finally {
      rulesLoading.value = false
    }
  }

  /**
   * 创建告警规则,成功后把后端返回的对象插入本地列表头部。
   *
   * @param req 创建请求
   * @returns 创建后的规则;失败时返回 null
   */
  async function createRule(req: CreateAlertRuleRequest): Promise<AlertRule | null> {
    try {
      const response = await createAlertRule(req)
      rules.value = [response.data, ...rules.value]
      return response.data
    } catch (reason) {
      rulesError.value = reason instanceof Error ? reason.message : '规则创建失败'
      return null
    }
  }

  /**
   * 更新告警规则,成功后用后端返回的对象替换列表中的旧条目。
   *
   * @param id 目标规则 ID
   * @param req 更新请求(仅含 threshold_value/level/enabled)
   */
  async function updateRule(id: number, req: UpdateAlertRuleRequest): Promise<AlertRule | null> {
    try {
      const response = await updateAlertRule(id, req)
      const index = rules.value.findIndex((rule) => rule.id === id)
      if (index >= 0) {
        const next = rules.value.slice()
        next[index] = response.data
        rules.value = next
      }
      return response.data
    } catch (reason) {
      rulesError.value = reason instanceof Error ? reason.message : '规则更新失败'
      return null
    }
  }

  /**
   * 软删除告警规则,成功后从本地列表移除。
   *
   * @param id 目标规则 ID
   */
  async function deleteRule(id: number): Promise<boolean> {
    try {
      await deleteAlertRule(id)
      rules.value = rules.value.filter((rule) => rule.id !== id)
      return true
    } catch (reason) {
      rulesError.value = reason instanceof Error ? reason.message : '规则删除失败'
      return false
    }
  }

  /**
   * 加载告警记录分页。
   * REST 始终为真源;无论 WS 是否连接,本调用都会刷新列表。
   *
   * @param query 查询参数;不传则使用 store 当前 query
   */
  async function loadRecords(query?: AlertRecordQuery): Promise<void> {
    const next = query ?? recordsQuery.value
    recordsQuery.value = next
    recordsLoading.value = true
    recordsError.value = null
    try {
      const response = await listAlertRecords({
        page: next.page ?? 1,
        page_size: next.page_size ?? DEFAULT_PAGE_SIZE,
        server_id: next.server_id,
        status: next.status
      })
      records.value = response.data.items
      recordsTotal.value = response.data.total
    } catch (reason) {
      recordsError.value = reason instanceof Error ? reason.message : '记录加载失败'
    } finally {
      recordsLoading.value = false
    }
  }

  /**
   * 标记告警记录已读,成功后把本地对应条目 `status` 改为 `read`。
   *
   * @param id 目标记录 ID
   */
  async function markRead(id: number): Promise<boolean> {
    try {
      await markAlertRecordAsRead(id)
      records.value = records.value.map((record) =>
        record.id === id && record.status === 'unread'
          ? { ...record, status: 'read' as const }
          : record
      )
      return true
    } catch (reason) {
      recordsError.value = reason instanceof Error ? reason.message : '标记已读失败'
      return false
    }
  }

  /**
   * 处理一次 WS `alert.push` 帧。
   *
   * 设计:仅递增 `pendingPushCount`,**不**把 push 数据直接合入 `records`。
   * 原因:push payload 是简化版 AlertRecord,不包含 read_by/read_at/created_at/message,
   * 强行合入会导致字段不一致;REST 是真源,顶部轻提示条触发用户主动刷新即可。
   *
   * @param payload 来自 `/ws/monitor` 的 `alert.push` 帧载荷
   */
  function applyAlertPush(payload: AlertPushPayload): void {
    if (!isAlertPush(payload)) return
    pendingPushCount.value += 1
  }

  /** 用户点击"刷新"后调用,把顶部提示计数清零。 */
  function markPendingPushSeen(): void {
    pendingPushCount.value = 0
  }

  /** WS 连接状态变化(由 AlertRecordsView 订阅并调用)。 */
  function setWsConnected(value: boolean): void {
    wsConnected.value = value
  }

  /** WS 错误文案(由 AlertRecordsView 捕获 socket 异常后调用)。 */
  function setWsError(message: string | null): void {
    wsError.value = message
  }

  function resetRules(): void {
    rules.value = []
    rulesLoading.value = false
    rulesError.value = null
  }

  function resetRecords(): void {
    records.value = []
    recordsTotal.value = 0
    recordsQuery.value = {}
    recordsLoading.value = false
    recordsError.value = null
    pendingPushCount.value = 0
  }

  return {
    // state
    rules,
    rulesLoading,
    rulesError,
    records,
    recordsTotal,
    recordsQuery,
    recordsLoading,
    recordsError,
    pendingPushCount,
    wsConnected,
    wsError,
    // actions
    loadRules,
    createRule,
    updateRule,
    deleteRule,
    loadRecords,
    markRead,
    applyAlertPush,
    markPendingPushSeen,
    setWsConnected,
    setWsError,
    resetRules,
    resetRecords
  }
})