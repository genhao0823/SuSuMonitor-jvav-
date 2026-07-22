# 开发日志: 前端 catch-up(B1) — 工程配置 + 类型常量 + 入口 + 全局样式

**日期**: 2026-07-22
**操作人**: opencode
**关联计划**: 历史 catch-up(回填未提交的前端基础)
**关联需求**: 各 M2-M6 模块的工程骨架

## 状态

工程配置、类型常量、入口、Pinia 安装 / axios 拦截器、Element Plus 中文、全局样式均已落库。
此 commit 不动业务视图、不动 router、不动 stores。

## 本次落地的工程文件

### 新增文件

- `web-vue-SuMon/tsconfig.json`(18 行)
  - 继承 `@vue/tsconfig/tsconfig.dom.json`
  - `strict: true` + `noImplicitAny` + `noUnusedLocals` + `noUnusedParameters` + `noFallthroughCasesInSwitch`
  - `@/*` alias 指向 `src/*`
  - `types: ["node"]`
- `web-vue-SuMon/tsconfig.node.json`(14 行)
  - 仅用于 `vite.config.ts`,ES2022 + Bundler + ESNext
- `web-vue-SuMon/vite.config.ts`(42 行)
  - 插件:`vue()` + AutoImport(Element Plus) + Components(Element Plus)
  - alias `@` → `./src`
  - dev server: `127.0.0.1:5173`,代理 `/api → http://localhost:18080`
  - build: `target: es2022`,`outDir: dist`,`sourcemap: false`
- `web-vue-SuMon/env.d.ts`(15 行)
  - `/// <reference types="vite/client" />`
  - `*.vue` 模块声明
  - `ImportMetaEnv.VITE_APP_TITLE` 类型
- `web-vue-SuMon/src/main.ts`(80 行)
  - `createApp` + Pinia + persistedstate + axios 拦截器回调
  - Element Plus 中文 locale
  - 全局 `errorHandler` 兜底(输出 console + ElMessage)
  - 启动时若已有 token 则静默 `auth.refresh()`
  - `enforceInitialAuth()` 启动期防御性二次校验
- `web-vue-SuMon/src/styles/global.css`(20 行)
  - 全局 reset(`html/body/#app` 100% 高度)
  - 涂山苏苏主色 CSS variables
  - `font-family` 缺省
- `web-vue-SuMon/src/types/api.d.ts`(172 行)
  - `ApiResponse<T>` 统一响应包装
  - `PageResult<T>` 分页
  - `HealthStatus` / `ReadyStatus`
  - `UserRole` / `ReviewStatus` / `CurrentUser` / `LoginResult`
  - `ServerStatusKind` / `AgentStatusKind` / `SshAuthType`
  - `Server` / `ServerStatus` / `ServerQuery` / `CreateServerRequest` / `UpdateServerRequest`
- `web-vue-SuMon/src/types/error-code.ts`(19 行)
  - `ErrorCode as const` 集中常量
  - 含 SUCCESS / 4xxxx / 5xxxx 全部错误码
- `web-vue-SuMon/src/types/metrics.ts`(19 行)
  - `MetricsLatest` / `MetricsHistory` 字段定义

## 关键设计要点

- **`strict: true` 全开**:不允许 implicit any、不允许未使用变量/参数,所有新代码必须通过此闸门。
- **字段命名按模块分开**:`auth` 模块用 camelCase(`reviewStatus`、`reviewedAt`),`server` 模块用 snake_case(`ssh_host`、`agent_id`)。两套命名不混,与各自 OpenAPI 对齐。
- **错误码常量集中化**:后端改码只改一处(`error-code.ts`),OpenAPI 校验脚本保证与 JSON 一致。
- **Pinia 启动 → axios 拦截器回调**:`useAuthStore()` 必须在 `pinia.use(persistedstate)` 之后,否则 localStorage 还原时序错乱。

## 验证清单

> 注:catch-up 各 commit 不单独跑 typecheck/lint,因为 src/views/* 等下一批文件未 add 时,import 链路一定挂。统一验证将在所有 catch-up commit 完成后跑一次。

| 检查 | 命令 | 时机 | 结果 |
|---|---|---|---|
| git status | `git status --short` | 每个 commit 后 | 只动本批文件 |
| 统一 typecheck | `npm run typecheck` | catch-up 全部完成后 | _待跑_ |
| 统一 lint | `npm run lint` | catch-up 全部完成后 | _待跑_ |
| OpenAPI 校验 | `npm run openapi:check` | pre-commit | 通过 3/3 |
| dev server | 5173 端口 | 持续运行 | LISTEN OK |

## 已知限制

- `noUnusedLocals` / `noUnusedParameters` 严格 → 后续 catch-up commit 必须无未用代码

## 风险与对策

| 风险 | 处理 |
|---|---|
| tsconfig 严格等级太高导致后续 catch-up 报错 | 在 B 全部完成后跑 typecheck 时统一修复,本批配置是共识,不预先调整 |
| `src/types/auto-imports.d.ts` / `components.d.ts` 被 `.gitignore` 排除 | 完全预期,不 commit,后续 dev server 启动时会自生成 |
| vue 文件引用 `@/*` 别名,Vite alias 必须存在 | `vite.config.ts` 已配 `alias: { '@': fileURLToPath(...) }` |

## 后续

- B2: 鉴权基础(api/client, stores/auth, api/auth, api/system, LoginView 等)
- B3: 主布局(MainLayout, DashboardView)
- B4: 服务器 CRUD(ServerListView, ServerDetailView, ServerFormDialog)
- B5: 管理补充(api/admin)
- B6: utils/format
- A: M6 接通(下一个 commit)
