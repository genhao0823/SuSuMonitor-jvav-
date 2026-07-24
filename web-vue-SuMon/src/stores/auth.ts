import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  getCurrentUser,
  loginUser as loginUserApi,
  logoutUser as logoutUserApi,
  registerUser as registerUserApi,
  type LoginRequestBody,
  type RegisterRequestBody
} from '@/api/auth'
import type { CurrentUser } from '@/types/api'

/**
 * 持久化键名:token/user 持久化键。
 * 未勾选"记住我"时,登录成功后该键会被立即清除,关闭浏览器后即退出。
 */
const PERSIST_KEY = 'susumonitor-auth'

/**
 * 认证 Pinia store。
 *
 * 持久化策略:
 * - 默认(记住我)将 token 与 user 写入 localStorage;
 * - 登录时若 `rememberMe=false`,登录成功后立即清除持久化键,
 *   本次会话内 store 仍持有 token,但刷新或关闭浏览器后自动退出。
 *
 * 不持久化 password、SSH 凭据等敏感字段。
 */
export const useAuthStore = defineStore(
  'auth',
  () => {
    const token = ref<string | null>(null)
    const user = ref<CurrentUser | null>(null)
    const rememberMe = ref<boolean>(true)

    const isAuthenticated = computed(() => token.value !== null)
    const isAdmin = computed(
      () => user.value !== null && user.value.role === 'admin'
    )
    const isApproved = computed(
      () => user.value !== null && user.value.reviewStatus === 'approved'
    )

    /**
     * 用登录接口结果填充 token 和 user,失败抛出 ApiBusinessError。
     * 当 `rememberMe=false` 时,登录完成后立即清空 localStorage 中的持久化键,
     * 使浏览器关闭后自动退出登录。
     *
     * @param body 登录请求
     * @param options.rememberMe true=记住我(localStorage), false=仅本次会话
     */
    async function login(
      body: LoginRequestBody,
      options: { rememberMe?: boolean } = {}
    ): Promise<void> {
      const response = await loginUserApi(body)
      token.value = response.data.token
      user.value = response.data.user
      if (options.rememberMe === false) {
        try {
          localStorage.removeItem(PERSIST_KEY)
        } catch {
          /* ignore */
        }
      } else {
        rememberMe.value = true
      }
    }

    /**
     * 注册并保持已登录用户对象,但不签发 token(注册独立于登录)。
     * 注册成功后引导用户前往 /login 完成登录。
     */
    async function register(body: RegisterRequestBody): Promise<void> {
      const response = await registerUserApi(body)
      token.value = null
      user.value = response.data
    }

    /**
     * 通过 /api/auth/me 拉取最新用户状态并刷新 store。
     * 用于页面刷新后恢复会话,以及感知后端状态变更(审核、角色变更)。
     */
    async function refresh(): Promise<void> {
      if (token.value === null) {
        return
      }
      const response = await getCurrentUser()
      user.value = response.data
    }

    /**
     * 清空本地 token 和 user,通知后端,完成无状态退出。
     * 即便后端调用失败,本地状态必须清空。
     */
    async function logout(): Promise<void> {
      try {
        if (token.value !== null) {
          await logoutUserApi()
        }
      } finally {
        token.value = null
        user.value = null
        try {
          localStorage.removeItem(PERSIST_KEY)
          sessionStorage.removeItem(PERSIST_KEY)
        } catch {
          /* ignore */
        }
      }
    }

    /**
     * 仅清空本地状态,不清除后端会话(用于 40100 触发或被踢)。
     */
    function clearLocal(): void {
      token.value = null
      user.value = null
      try {
        localStorage.removeItem(PERSIST_KEY)
        sessionStorage.removeItem(PERSIST_KEY)
      } catch {
        /* ignore */
      }
    }

    return {
      token,
      user,
      rememberMe,
      isAuthenticated,
      isAdmin,
      isApproved,
      login,
      register,
      refresh,
      logout,
      clearLocal
    }
  },
  {
    persist: {
      key: PERSIST_KEY,
      storage: localStorage,
      pick: ['token', 'user']
    }
  }
)