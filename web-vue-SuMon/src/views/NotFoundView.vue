<template>
  <div class="not-found-view">
    <el-result
      :icon="isPlaceholder ? 'info' : 'warning'"
      :title="resultTitle"
      :sub-title="resultSubTitle"
    >
      <template #extra>
        <el-button
          type="primary"
          @click="goHome"
        >
          返回仪表盘
        </el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()

/**
 * 判断当前路由是否是占位路由（meta.placeholder=true）。
 * 占位路由当前用本组件渲染,但展示"敬请期待"文案而非 404。
 */
const isPlaceholder = computed<boolean>(
  () => route.meta.placeholder === true
)

const placeholderHint = computed<string>(() => {
  const hint = route.meta.placeholderHint
  return typeof hint === 'string' ? hint : '该功能尚在后续里程碑实现'
})

const resultTitle = computed<string>(() =>
  isPlaceholder.value ? '功能开发中' : '404'
)

const resultSubTitle = computed<string>(() =>
  isPlaceholder.value ? placeholderHint.value : '请求的页面不存在'
)

/**
 * 导航回仪表盘首页,统一作为兜底恢复路径。
 */
function goHome(): void {
  void router.push('/dashboard')
}
</script>

<style scoped>
.not-found-view {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 400px;
}
</style>