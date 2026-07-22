<template>
  <div class="forbidden-view">
    <el-result
      icon="error"
      title="403 无权限访问"
    >
      <template #sub-title>
        <p>您的账户没有访问该资源的权限。</p>
        <p v-if="auth.user">
          当前账户:<strong>{{ auth.user.username }}</strong>,
          角色:<strong>{{ auth.user.role }}</strong>,
          审核状态:<strong>{{ auth.user.reviewStatus }}</strong>
        </p>
      </template>
      <template #extra>
        <el-button
          type="primary"
          @click="goDashboard"
        >
          返回仪表盘
        </el-button>
        <el-button @click="handleLogout">
          退出登录
        </el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

async function goDashboard(): Promise<void> {
  await router.push({ name: 'dashboard' })
}

async function handleLogout(): Promise<void> {
  await auth.logout()
  ElMessage.success('已退出登录')
  await router.push({ name: 'login' })
}
</script>

<style scoped>
.forbidden-view {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  background: var(--el-bg-color-page);
}
</style>