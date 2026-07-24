# 开发日志: 前端 catch-up(B2) — M2 鉴权基础

**日期**: 2026-07-22
**操作人**: opencode
**关联计划**: 历史 catch-up
**关联需求**: M2 登录/注册/当前用户/退出 + 路由守卫

## 状态

M2 鉴权基础全部已落库。
本次 catch-up 不引入新功能,仅回填未提交的文件。

## 本次落地的工程文件

### 新增文件(11 个 / 2018 行)

| 文件 | 行数 | 说明 |
|---|---|---|
| `web-vue-SuMon/src/api/client.ts` | 159 | axios 实例 + 拦截器 + `ApiBusinessError` + `setApiClientCallbacks` |
| `web-vue-SuMon/src/api/auth.ts` | 68 | `login`/`register`/`logout`/`currentUser` 4 个函数 |
| `web-vue-SuMon/src/api/system.ts` | 20 | `health`/`ready` 2 个函数 |
| `web-vue-SuMon/src/stores/auth.ts` | 148 | Pinia auth store + `persistedstate` |
| `web-vue-SuMon/src/router/guards.ts` | 57 | `installRouterGuards` 处理 requiresAuth/requiresAdmin/publicOnly |
| `web-vue-SuMon/src/views/LoginView.vue` | 269 | 登录表单 + 涂山苏苏视觉 |
| `web-vue-SuMon/src/views/RegisterView.vue` | 239 | 注册表单 + 审核状态提示 |
| `web-vue-SuMon/src/views/AuthLayout.vue` | 848 | 公共 auth 布局(背景 + 签名引言池 + 铃铛花瓣) |
| `web-vue-SuMon/src/views/ForbiddenView.vue` | 58 | 403 提示页 |
| `web-vue-SuMon/src/views/NotFoundView.vue` | 64 | 404 提示页 |
| `web-vue-SuMon/src/components/TushanFoxMark.vue` | 88 | 涂山苏苏签名图标组件 |

## 关键设计要点

- **`ApiBusinessError`**:自定义错误类,承载后端 `code` + `message`,拦截器统一抛出。
  组件层 `catch (error)`,通过 `error.code === ErrorCode.XXX` 走精细分支。
- **拦截器回调启动期未就绪**:`setApiClientCallbacks({...})` 必须在 Pinia 安装后调,
  所以放在 `main.ts:31`,而不是模块顶层。
- **`installRouterGuards`** 文档化流程:未登录跳 `/login?redirect=...`;已登录访问
  `/login`、`/register` 反送 `/dashboard`,避免重复登录。
- **涂山苏苏视觉系统**:统一在 `AuthLayout.vue` 实现,含背景图、签名引言池(随机抽取
  一句涂山苏苏语录)、铃铛花瓣波浪动画。

## 已知限制

- `noUnusedLocals` 严格 → 无未用变量保证
- 涂山 IP 仅 demo,`README.md` 涂山 IP 使用范围段必读

## 后续

- B3: 主布局 MainLayout + DashboardView
- B4: 服务器 CRUD(ServerListView, ServerDetailView, ServerFormDialog)
