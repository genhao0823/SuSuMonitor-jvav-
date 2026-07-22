import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import { ElMessage } from 'element-plus'
import router from './router'
import App from './App.vue'
import { setApiClientCallbacks } from './api/client'
import { useAuthStore } from './stores/auth'

import 'element-plus/dist/index.css'
import './styles/global.css'

const app = createApp(App)
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)

/**
 * 注册全局未捕获异常兜底:写入控制台并向用户提示通用消息。
 * 组件内业务异常不应依赖此处。
 */
app.config.errorHandler = (error, _instance, info) => {
  // eslint-disable-next-line no-console
  console.error('[GlobalError]', info, error)
  ElMessage.error('应用发生未预期错误,请刷新或联系管理员')
}

// 拦截器回调必须放在 Pinia 安装之后,以便 useAuthStore 可用。
const auth = useAuthStore()
setApiClientCallbacks({
  onUnauthorized: () => {
    auth.clearLocal()
    const target = router.currentRoute.value.fullPath
    if (target !== '/login') {
      void router.push({ name: 'login', query: { redirect: target } })
    }
  },
  onForbidden: () => {
    if (router.currentRoute.value.name !== 'forbidden') {
      void router.push({ name: 'forbidden' })
    }
  }
})

// 启动时若已有持久化 token,后台静默刷新一次当前用户,确保角色/审核状态最新。
if (auth.isAuthenticated) {
  auth.refresh().catch(() => {
    /* 拦截器已处理 40100,此处静默 */
  })
}

/**
 * 启动期防御性二次校验:
 * 路由守卫 beforeEach 已经在跳转前完成检查,但如果有任何代码路径未经过守卫
 * 直接命中受保护路由(App.vue 直接 router.push 等),这里的二次检查会兜底。
 * 实测中通常不触发,留着是为了序列化 hydration 与导航的极端边缘场景。
 */
function enforceInitialAuth(): void {
  const current = router.currentRoute.value
  const requiresAuth = current.meta.requiresAuth === true
  const requiresAdmin = current.meta.requiresAdmin === true
  if (!requiresAuth && !requiresAdmin) {
    return
  }
  if (requiresAuth && !auth.isAuthenticated) {
    void router.replace({
      name: 'login',
      query: { redirect: current.fullPath }
    })
    return
  }
  if (requiresAdmin && !auth.isAdmin) {
    void router.replace({ name: 'forbidden' })
  }
}

app.use(router)
app.mount('#app')
enforceInitialAuth()