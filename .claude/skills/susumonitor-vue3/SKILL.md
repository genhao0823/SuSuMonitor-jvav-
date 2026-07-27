---
name: susumonitor-vue3
description: SuSuMonitor Vue 3 前端编码规范。当在 SuSuMonitor 项目 web-vue-SuMon/ 目录下编写 Vue 3 前端代码时自动加载。触发场景：编写 Vue 组件、使用 Pinia 状态管理、配置 Vue Router、使用 Element Plus 组件库、编写前端样式。
when_to_use: 编写 Vue 3 组件 / Pinia store / 路由 / Element Plus / 前端样式时
user-invocable: true
---

# SuSuMonitor Vue 3 编码规范

## 1. 组件风格

- 统一使用 `<script setup>` 语法（Composition API）
- 组件名使用 PascalCase，如 `ServerList.vue`
- 模板中使用 kebab-case 引用组件，如 `<server-list />`

## 2. 组件命名

- 页面组件放在 `src/views/`，命名 `XxxView.vue`
- 公共组件放在 `src/components/`，命名 `XxxComponent.vue`
- 布局组件放在 `src/layouts/`

## 3. 状态管理 (Pinia)

- Store 文件放在 `src/stores/`
- 使用 `defineStore` 定义，命名 `useXxxStore`
- 使用 `ref` 定义状态，`computed` 定义 getter
- 异步操作放在 `actions` 中
- 持久化通过 `pinia-plugin-persistedstate` 选择性启用（仅 token / user 等需要跨刷新的状态）

## 4. 路由 (Vue Router)

- 路由配置放在 `src/router/`
- 需要登录才能访问的页面使用路由守卫 `beforeEach`
- 路由命名使用 kebab-case
- meta 字段约定：`requiresAuth` / `requiresAdmin` / `publicOnly` / `title`

## 5. Element Plus

- 组件使用前缀 `El`，如 `<el-button>`
- 图标使用 `@element-plus/icons-vue`
- 表单校验使用 el-form 的 rules 属性
- 按需引入通过 `unplugin-auto-import` + `unplugin-vue-components` + `ElementPlusResolver` 配置

## 6. 样式

- 使用 `<style scoped>` 避免样式污染
- 颜色使用 Element Plus 的 CSS 变量（`--el-color-primary` 等）
- 优先使用 flex/grid 布局

## 7. API 调用

- 统一封装在 `src/api/` 目录下
- 使用 axios 实例，配置 baseURL 和拦截器
- 请求拦截器添加 `Authorization: Bearer <JWT>` + `X-Correlation-ID`
- 响应拦截器统一处理 40100/40300 跳转（`src/api/client.ts`）

## 8. 目录结构

```
web-vue-SuMon/src/
├── api/          # API 请求封装
├── assets/       # 静态资源
├── components/   # 公共组件
├── composables/  # 组合式函数
├── layouts/      # 布局组件
├── router/       # 路由配置
├── services/     # 长连接（WebSocket 等）
├── stores/       # Pinia 状态管理
├── styles/       # 全局样式
├── types/        # TS 类型定义（对齐 OpenAPI）
├── utils/        # 工具函数
├── views/        # 页面组件
├── App.vue
└── main.ts
```

## 9. 其他

- 使用 `const` 而非 `let`，不使用 `var`
- 使用箭头函数作为回调
- 使用 ES6 模块 `import/export`
- 提交前确保 `npm run typecheck` + `lint` + `test` + `openapi:check` + `audit:catchup` 全部通过
- OpenAPI JSON 是唯一事实源，类型定义以 `src/types/api.d.ts` 为准
