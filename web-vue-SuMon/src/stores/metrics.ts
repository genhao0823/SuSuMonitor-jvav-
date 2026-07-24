import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getLatestMetrics, getMetricsHistory } from '@/api/metrics'
import type { MetricsHistory, MetricsLatest } from '@/types/metrics'

/** 管理监控页面的最新值、历史数据和实时连接状态。 */
export const useMetricsStore = defineStore('metrics', () => {
  const latest = ref<MetricsLatest | null>(null)
  const history = ref<MetricsHistory[]>([])
  const loading = ref(false)
  const connected = ref(false)
  const error = ref<string | null>(null)

  async function load(serverId: number, startTime: string, endTime: string): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const [latestResponse, historyResponse] = await Promise.all([
        getLatestMetrics(serverId),
        getMetricsHistory(serverId, startTime, endTime)
      ])
      latest.value = latestResponse.data
      history.value = historyResponse.data.items
    } catch (reason) {
      error.value = reason instanceof Error ? reason.message : '指标加载失败'
    } finally {
      loading.value = false
    }
  }

  function applyRealtime(value: MetricsLatest): void {
    latest.value = value
  }

  function setConnected(value: boolean): void {
    connected.value = value
  }

  function reset(): void {
    latest.value = null
    history.value = []
    connected.value = false
    error.value = null
  }

  return { latest, history, loading, connected, error, load, applyRealtime, setConnected, reset }
})
