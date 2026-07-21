import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import type { Router } from 'vue-router'

/**
 * 把 NProgress 进度条绑到 Vue Router 生命周期。
 *
 * - 路由切换开始 → 启动进度条
 * - 切换完成/失败 → 关闭进度条
 *
 * 必须在 `createRouter` 之后调,否则 hooks 未注册,NProgress 不会触发。
 *
 * @param router Vue Router 实例
 */
export function installRouterLoading(router: Router): void {
  NProgress.configure({
    showSpinner: false,
    trickleSpeed: 200,
    minimum: 0.15
  })
  router.beforeEach(() => {
    NProgress.start()
  })
  router.afterEach(() => {
    NProgress.done()
  })
  router.onError(() => {
    NProgress.done()
  })
}