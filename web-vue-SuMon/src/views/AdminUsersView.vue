<template>
  <div class="admin-users-view">
    <PageHeader
      title="用户审核"
      subtitle="处理待审核用户的注册申请;通过或拒绝用户注册请求"
    >
      <template #actions>
        <el-button
          :loading="refreshing"
          class="admin-users-view__refresh"
          aria-label="刷新待审核列表"
          @click="reload"
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
          <span>刷新</span>
        </el-button>
      </template>
    </PageHeader>

    <el-card
      class="admin-users-view__card"
      shadow="never"
    >
      <div class="admin-users-view__summary">
        <span class="admin-users-view__summary-label">待审核</span>
        <el-tag
          :type="pendingList.length > 0 ? 'warning' : 'info'"
          effect="dark"
          size="default"
        >
          {{ pendingList.length }} 人
        </el-tag>
        <el-input
          v-model="searchKeyword"
          placeholder="按用户名过滤"
          clearable
          class="admin-users-view__search"
        />
      </div>

      <el-table
        v-loading="loading"
        :data="filteredPendingList"
        stripe
        class="admin-users-view__table"
        :empty-text="searchKeyword.length > 0 ? '无匹配用户' : '暂无待审核用户,所有申请已处理完毕'"
      >
        <el-table-column
          prop="id"
          label="ID"
          width="80"
        />
        <el-table-column
          label="用户名"
          min-width="200"
        >
          <template #default="{ row }">
            <strong>{{ (row as CurrentUser).username }}</strong>
          </template>
        </el-table-column>
        <el-table-column
          label="注册时间"
          width="220"
        >
          <template #default="{ row }">
            {{ formatDateTime((row as CurrentUser).createdAt) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="280"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              size="small"
              type="success"
              :loading="busyId === row.id && busyAction === 'approve'"
              :disabled="busyId !== null && busyId !== row.id"
              @click="approve(row as CurrentUser)"
            >
              通过
            </el-button>
            <el-popconfirm
              :title="`确定拒绝 ${(row as CurrentUser).username} 吗?`"
              confirm-button-text="拒绝"
              cancel-button-text="取消"
              @confirm="reject(row as CurrentUser)"
            >
              <template #reference>
                <el-button
                  size="small"
                  type="danger"
                  plain
                  :loading="busyId === row.id && busyAction === 'reject'"
                  :disabled="busyId !== null && busyId !== row.id"
                >
                  拒绝
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { ApiBusinessError } from '@/api/client'
import { approveUser, listPendingUsers, rejectUser } from '@/api/admin'
import { ErrorCode } from '@/types/error-code'
import type { CurrentUser } from '@/types/api'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const refreshing = ref(false)
/**
 * 当前正在处理的行 id + 动作,用于按钮级 loading。
 * busyId 非空时,其他行的操作按钮被禁用,防止并发审核混乱。
 */
const busyId = ref<number | null>(null)
const busyAction = ref<'approve' | 'reject' | null>(null)

const pendingList = ref<CurrentUser[]>([])

/**
 * 用户名本地过滤关键字。后端暂无 /api/admin/users/search 端点,
 * 此处纯前端 filter;待后端搜索接口就绪后可平滑替换为远端搜索。
 */
const searchKeyword = ref('')

/**
 * 按 searchKeyword 过滤后的待审核列表。
 * 不区分大小写,trim 关键字;空关键字返回原列表(避免无谓过滤)。
 */
const filteredPendingList = computed<CurrentUser[]>(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (keyword.length === 0) {
    return pendingList.value
  }
  return pendingList.value.filter((u) => u.username.toLowerCase().includes(keyword))
})

/**
 * 拉取待审核列表。
 */
async function fetchPending(): Promise<void> {
  loading.value = true
  try {
    const response = await listPendingUsers()
    pendingList.value = Array.isArray(response.data) ? response.data : []
  } finally {
    loading.value = false
  }
}

/**
 * 重新加载(顶层刷新按钮用)。
 */
async function reload(): Promise<void> {
  if (refreshing.value) {
    return
  }
  refreshing.value = true
  try {
    await fetchPending()
  } catch (error) {
    ElMessage.error(explainError(error))
  } finally {
    refreshing.value = false
  }
}

/**
 * 通过用户。无二次确认 — admin 频繁审批场景下多一步会拖慢。
 */
async function approve(user: CurrentUser): Promise<void> {
  busyId.value = user.id
  busyAction.value = 'approve'
  try {
    await approveUser(user.id)
    ElMessage.success(`已通过 ${user.username}`)
    await fetchPending()
  } catch (error) {
    ElMessage.error(explainError(error))
  } finally {
    busyId.value = null
    busyAction.value = null
  }
}

/**
 * 拒绝用户(el-popconfirm 已二次确认)。
 */
async function reject(user: CurrentUser): Promise<void> {
  busyId.value = user.id
  busyAction.value = 'reject'
  try {
    await rejectUser(user.id)
    ElMessage.success(`已拒绝 ${user.username}`)
    await fetchPending()
  } catch (error) {
    ElMessage.error(explainError(error))
  } finally {
    busyId.value = null
    busyAction.value = null
  }
}

/**
 * ApiBusinessError → 用户提示。
 * 错误码语义参考 OpenAPI ErrorResponse + 后端实际探测结果。
 */
function explainError(error: unknown): string {
  if (error instanceof ApiBusinessError) {
    if (error.code === ErrorCode.RESOURCE_CONFLICT) {
      return '该用户已被审核,请刷新列表查看最新状态'
    }
    if (error.code === ErrorCode.RESOURCE_NOT_FOUND) {
      return '用户不存在或已被删除'
    }
    if (error.code === ErrorCode.FORBIDDEN) {
      return '当前账号无权操作'
    }
    if (error.code === ErrorCode.INVALID_REQUEST_PARAMETER) {
      return '参数不合法'
    }
    return error.message || '操作失败'
  }
  return '网络异常,请稍后重试'
}

onMounted(() => {
  void fetchPending().catch((error) => ElMessage.error(explainError(error)))
})
</script>

<style scoped>
.admin-users-view {
  max-width: 1100px;
  margin: 0 auto;
}

.admin-users-view__card {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 16px;
  box-shadow:
    0 12px 32px rgba(183, 50, 92, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

.admin-users-view__card :deep(.el-card__body) {
  padding: 20px;
}

.admin-users-view__summary {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.admin-users-view__search {
  max-width: 280px;
  margin-left: auto;
}

.admin-users-view__summary-label {
  font-size: 14px;
  font-weight: 600;
  color: #2a1626;
}

.admin-users-view__refresh {
  background: linear-gradient(135deg, #ff5b8a 0%, #b7325c 100%) !important;
  border: none !important;
  color: #fff !important;
  box-shadow: 0 6px 14px rgba(255, 91, 138, 0.3) !important;
}

.admin-users-view__refresh:hover {
  background: linear-gradient(135deg, #ff7aa3 0%, #c8426f 100%) !important;
  box-shadow: 0 10px 20px rgba(255, 91, 138, 0.45) !important;
}

.admin-users-view__refresh svg {
  width: 16px;
  height: 16px;
  margin-right: 4px;
  vertical-align: -2px;
}

.admin-users-view__table {
  border-radius: 8px;
  overflow: hidden;
}
</style>