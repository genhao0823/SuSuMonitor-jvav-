<template>
  <div class="server-list-view">
    <PageHeader
      title="服务器列表"
      subtitle="管理已注册的服务器资产,只有管理员可以增删改,普通用户可以查看"
    >
      <template #actions>
        <el-button
          type="primary"
          class="server-list-view__create"
          @click="openCreate"
        >
          <svg
            viewBox="0 0 24 24"
            xmlns="http://www.w3.org/2000/svg"
            aria-hidden="true"
          >
            <path
              d="M12 4 L12 20 M4 12 L20 12"
              fill="none"
              stroke="currentColor"
              stroke-width="2.4"
              stroke-linecap="round"
            />
          </svg>
          <span>创建服务器</span>
        </el-button>
      </template>
    </PageHeader>

    <el-card
      class="server-list-view__card"
      shadow="never"
    >
      <div class="server-list-view__toolbar">
        <el-input
          v-model="searchName"
          placeholder="按名称搜索"
          clearable
          class="server-list-view__search"
        >
          <template #prefix>
            <svg
              viewBox="0 0 24 24"
              xmlns="http://www.w3.org/2000/svg"
              aria-hidden="true"
            >
              <circle
                cx="11"
                cy="11"
                r="6"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              />
              <path
                d="M16 16 L20 20"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
              />
            </svg>
          </template>
        </el-input>
        <el-input
          v-model="searchHost"
          placeholder="按主机地址搜索"
          clearable
          class="server-list-view__search"
        >
          <template #prefix>
            <svg
              viewBox="0 0 24 24"
              xmlns="http://www.w3.org/2000/svg"
              aria-hidden="true"
            >
              <circle
                cx="11"
                cy="11"
                r="6"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              />
              <path
                d="M16 16 L20 20"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
              />
            </svg>
          </template>
        </el-input>
        <el-select
          v-model="pageSize"
          class="server-list-view__page-size"
          @change="onPageSizeChange"
        >
          <el-option
            v-for="opt in pageSizeOptions"
            :key="opt"
            :label="`${opt} 条/页`"
            :value="opt"
          />
        </el-select>
        <el-button @click="reload">
          刷新
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="pagedRows"
        stripe
        class="server-list-view__table"
        empty-text="暂无服务器,点击右上角创建"
        @sort-change="onSortChange"
      >
        <el-table-column
          prop="id"
          label="ID"
          width="80"
          sortable="custom"
        />
        <el-table-column
          prop="name"
          label="名称"
          min-width="160"
        >
          <template #default="{ row }">
            <router-link
              :to="{ name: 'server-detail', params: { serverId: row.id } }"
              class="server-list-view__name-link"
            >
              {{ row.name }}
            </router-link>
          </template>
        </el-table-column>
        <el-table-column
          prop="host"
          label="主机"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          label="状态"
          width="110"
        >
          <template #default="{ row }">
            <span
              class="server-list-view__status"
              :class="`server-list-view__status--${row.status}`"
            >
              {{ serverStatusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          label="Agent"
          width="110"
        >
          <template #default="{ row }">
            <span
              class="server-list-view__agent"
              :class="`server-list-view__agent--${row.agent_status}`"
            >
              {{ row.agent_status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          prop="ssh_port"
          label="SSH"
          width="90"
        >
          <template #default="{ row }">
            {{ row.ssh_user }}@:{{ row.ssh_port }}
          </template>
        </el-table-column>
        <el-table-column
          prop="created_at"
          label="创建时间"
          width="180"
          sortable="custom"
        >
          <template #default="{ row }">
            {{ formatDateTime(row.created_at) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="320"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              size="small"
              @click="goDetail(row as Server)"
            >
              详情
            </el-button>
            <el-button
              size="small"
              type="primary"
              plain
              @click="openEdit(row as Server)"
            >
              编辑
            </el-button>
            <el-button
              size="small"
              plain
              @click="handleTestConnection(row as Server)"
            >
              测试连接
            </el-button>
            <el-popconfirm
              :title="`确定要删除 ${(row as Server).name} 吗?`"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="handleDelete(row as Server)"
            >
              <template #reference>
                <el-button
                  size="small"
                  type="danger"
                  plain
                >
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="server-list-view__pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="totalCount"
          :page-sizes="pageSizeOptions"
          layout="total, prev, pager, next, jumper"
          background
          @current-change="onPageChange"
        />
      </div>
    </el-card>

    <ServerFormDialog
      v-model="dialogVisible"
      :server="editingServer"
      @success="reload"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import ServerFormDialog from '@/components/ServerFormDialog.vue'
import { ApiBusinessError } from '@/api/client'
import { deleteServer, listServers } from '@/api/server'
import { ErrorCode } from '@/types/error-code'
import type { Server, ServerQuery } from '@/types/api'
import { formatDateTime, serverStatusLabel } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
/**
 * 后端拉到的原始顺序数据(后端忽略排序参数,默认按 id desc)。
 * 客户端排序在此数据副本上做,不修改引用。
 * 注意:这是 M4 临时方案。后端排序修好后(Bug-fix 文档记录),
 * 应删除本字段和 sortedRows/pagedRows/totalCount,恢复调 reload()。
 */
const serverItems = ref<Server[]>([])

const page = ref(1)
const pageSizeOptions: number[] = [10, 20, 50]
const pageSize = ref<number>(pageSizeOptions[0])
const searchName = ref('')
const searchHost = ref('')
/**
 * 排序状态。被 sortedRows computed 读取;不再触发网络请求。
 */
const sortBy = ref<ServerQuery['sort_by']>('id')
const sortOrder = ref<ServerQuery['sort_order']>('desc')

const dialogVisible = ref(false)
const editingServer = ref<Server | null>(null)

/**
 * 构建查询参数对象。
 * page_size 设为 100(后端 OpenAPI 约束 max=100,openapi-server.json:543),一次拉全,前端做分页;
 * 后端排序参数仍发(后端忽略无害)。
 */
function buildQuery(): ServerQuery {
  const q: ServerQuery = {
    page: 1,
    page_size: 100,
    sort_by: sortBy.value,
    sort_order: sortOrder.value
  }
  const name = searchName.value.trim()
  const host = searchHost.value.trim()
  if (name.length > 0) {
    q.name = name
  }
  if (host.length > 0) {
    q.host = host
  }
  return q
}

/**
 * 客户端排序后的全量数据。
 * 默认状态(id desc 与后端默认一致)直接复用 serverItems 避免无谓复制。
 */
const sortedRows = computed<Server[]>(() => {
  const data = serverItems.value
  if (data.length === 0) {
    return data
  }
  const prop = sortBy.value
  const dir = sortOrder.value
  if (prop === 'id' && dir === 'desc') {
    return data
  }
  const copy = data.slice()
  copy.sort((a, b) => {
    const av = (a as unknown as Record<string, unknown>)[prop as string]
    const bv = (b as unknown as Record<string, unknown>)[prop as string]
    let cmp = 0
    if (typeof av === 'number' && typeof bv === 'number') {
      cmp = av - bv
    } else {
      cmp = String(av ?? '').localeCompare(String(bv ?? ''), 'zh-CN')
    }
    return dir === 'asc' ? cmp : -cmp
  })
  return copy
})

/**
 * 当前页可见数据(前端分页)。
 */
const pagedRows = computed<Server[]>(() => {
  const start = (page.value - 1) * pageSize.value
  return sortedRows.value.slice(start, start + pageSize.value)
})

/**
 * 总数 = 客户端全量长度,用于分页器显示。
 */
const totalCount = computed<number>(() => serverItems.value.length)

/**
 * 拉取服务器列表(全量)。失败抛出 ApiBusinessError。
 */
async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const response = await listServers(buildQuery())
    serverItems.value = response.data?.items ?? []
  } finally {
    loading.value = false
  }
}

/**
 * 重新加载当前查询条件的列表。
 */
async function reload(): Promise<void> {
  try {
    await fetchList()
  } catch (error) {
    ElMessage.error(explainError(error))
  }
}

/**
 * 排序切换:仅更新本地状态,触发 sortedRows 重算。
 */
function onSortChange(sort: { prop: string | null; order: 'ascending' | 'descending' | null }): void {
  if (sort.order === null || sort.prop === null) {
    sortBy.value = 'id'
    sortOrder.value = 'desc'
  } else {
    sortBy.value = sort.prop as ServerQuery['sort_by']
    sortOrder.value = sort.order === 'ascending' ? 'asc' : 'desc'
  }
  page.value = 1
}

/**
 * 分页器页码变化(纯本地)。
 */
function onPageChange(_page: number): void {
  // el-pagination v-model:current-page 已自动更新 page.value
  // 此函数占位以接收事件,无需调后端
}

/**
 * 切换每页大小:仅更新本地状态,前端切片立即生效。
 */
function onPageSizeChange(size: number): void {
  pageSize.value = size
  page.value = 1
}

function openCreate(): void {
  editingServer.value = null
  dialogVisible.value = true
}

function openEdit(row: Server): void {
  editingServer.value = row
  dialogVisible.value = true
}

function goDetail(row: Server): void {
  void router.push({ name: 'server-detail', params: { serverId: row.id } })
}

/**
 * SSH 测试连接按钮:目前后端 MVP-7 才接通真实实现,
 * 这里只展示占位文案,不让用户误以为已发起真实连接。
 */
function handleTestConnection(_row: Server): void {
  ElMessage.info('测试连接功能将在 MVP-7 接入,敬请期待')
}

/**
 * 软删除服务器,删除成功刷新列表(全量重拉),失败提示。
 */
async function handleDelete(row: Server): Promise<void> {
  try {
    await deleteServer(row.id)
    ElMessage.success(`已删除 ${row.name}`)
    if (pagedRows.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await reload()
  } catch (error) {
    ElMessage.error(explainError(error))
  }
}

/**
 * 把 ApiBusinessError 翻译为用户可见提示。
 */
function explainError(error: unknown): string {
  if (error instanceof ApiBusinessError) {
    if (error.code === ErrorCode.RESOURCE_NOT_FOUND) {
      return '服务器不存在或已被删除'
    }
    if (error.code === ErrorCode.FORBIDDEN) {
      return '当前账号无权操作'
    }
    return error.message || '操作失败'
  }
  return '网络异常,请稍后重试'
}

onMounted(() => {
  void reload()
})
</script>

<style scoped>
.server-list-view {
  max-width: 1280px;
  margin: 0 auto;
}

.server-list-view__card {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 16px;
  box-shadow:
    0 12px 32px rgba(183, 50, 92, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

.server-list-view__card :deep(.el-card__body) {
  padding: 20px;
}

.server-list-view__create {
  background: linear-gradient(135deg, #ff5b8a 0%, #b7325c 100%) !important;
  border: none !important;
  box-shadow: 0 6px 14px rgba(255, 91, 138, 0.3) !important;
}

.server-list-view__create:hover {
  background: linear-gradient(135deg, #ff7aa3 0%, #c8426f 100%) !important;
  box-shadow: 0 10px 20px rgba(255, 91, 138, 0.45) !important;
}

.server-list-view__create svg {
  width: 16px;
  height: 16px;
  margin-right: 4px;
  vertical-align: -2px;
}

.server-list-view__toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.server-list-view__search {
  flex: 1;
  min-width: 180px;
  max-width: 280px;
}

.server-list-view__search :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.85);
}

.server-list-view__search :deep(.el-input__prefix svg) {
  width: 14px;
  height: 14px;
}

.server-list-view__page-size {
  width: 130px;
}

.server-list-view__table {
  border-radius: 8px;
  overflow: hidden;
}

.server-list-view__name-link {
  color: #b7325c;
  text-decoration: none;
  font-weight: 600;
}

.server-list-view__name-link:hover {
  color: #ff5b8a;
  text-decoration: underline;
}

.server-list-view__status,
.server-list-view__agent {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #fff;
}

.server-list-view__status--online,
.server-list-view__agent--online {
  background: linear-gradient(135deg, #66e6a8 0%, #2eb872 100%);
}

.server-list-view__status--offline,
.server-list-view__agent--offline {
  background: linear-gradient(135deg, #9ca3af 0%, #6b7280 100%);
}

.server-list-view__status--unknown,
.server-list-view__agent--unknown {
  background: linear-gradient(135deg, #f5b942 0%, #d97706 100%);
}

.server-list-view__pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>