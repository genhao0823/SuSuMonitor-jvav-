# SuSuMonitor Web 前端

SuSuMonitor 监控平台的 Web 前端工程，基于 Vue 3 + Vite + Element Plus。

## 当前里程碑

**M1 工程骨架** —— 仅含依赖、构建、代理、入口与 Dashboard 占位视图，真实业务页面在后续 M2-M7 中实现。

详细计划：[`docs-SuMon/Develop-plans/20260719-Web前端并行开发计划.md`](../docs-SuMon/Develop-plans/20260719-Web前端并行开发计划.md)
开发日志：[`docs-SuMon/Develop-log/20260719-Web前端工程初始化.md`](../docs-SuMon/Develop-log/20260719-Web前端工程初始化.md)

## 技术栈

| 类别 | 选型 |
|---|---|
| 构建 | Vite 5 |
| 框架 | Vue 3.5（`<script setup>` + Composition API） |
| 语言 | TypeScript 5 |
| 路由 | Vue Router 4 |
| 状态 | Pinia 2（含持久化插件） |
| 组件库 | Element Plus 2（按需引入） |
| HTTP | axios 1 |
| 代码规范 | ESLint 8 + Prettier 3 |

## 前置条件

- Node.js >= 18.18（推荐 LTS）
- npm >= 9
- 后端 `server-java-SuMon/` 已在 `localhost:18080` 运行并完成 Flyway 迁移

## 启动

```powershell
Set-Location "C:\Users\genhaosan\Desktop\SuSuMonitor(Jvav)\web-vue-SuMon"
npm install
npm run dev
```

默认开发服务器：[http://127.0.0.1:5173](http://127.0.0.1:5173)

Vite 已配置代理 `/api → http://localhost:18080`，前端直接以 `/api/*` 形式调用后端即可，无需关心跨域。

## 常用命令

| 命令 | 说明 |
|---|---|
| `npm run dev` | 启动 Vite 开发服务器（5173） |
| `npm run build` | 类型检查 + 生产构建到 `dist/` |
| `npm run preview` | 本地预览构建产物 |
| `npm run typecheck` | 仅类型检查 |
| `npm run lint` | ESLint 检查 |
| `npm run lint:fix` | ESLint 自动修复 |
| `npm run format` | Prettier 格式化 |

## 目录结构

```text
web-vue-SuMon/
├── .gitignore
├── .eslintrc.cjs
├── .prettierrc.json
├── package.json
├── tsconfig.json
├── tsconfig.node.json
├── vite.config.ts
├── index.html
├── env.d.ts
├── README.md
└── src/
    ├── main.ts
    ├── App.vue
    ├── styles/global.css
    ├── api/                  # src/api/ 在 M2 阶段创建
    ├── stores/               # src/stores/ 在 M2 阶段创建
    ├── router/
    │   └── index.ts
    ├── layouts/              # src/layouts/ 在 M3 阶段创建
    ├── components/           # src/components/ 在 M3 阶段创建
    ├── views/
    │   ├── DashboardView.vue (M1 占位)
    │   └── NotFoundView.vue
    ├── utils/                # src/utils/ 在 M3 阶段创建
    └── types/                # src/types/ 在 M2 阶段创建
```

## 与后端契约

后端 OpenAPI JSON 是唯一事实源，前端字段定义必须与之保持一致：

- `docs-SuMon/OpenApi-SuMon/openapi-system.json`
- `docs-SuMon/OpenApi-SuMon/openapi-auth.json`
- `docs-SuMon/OpenApi-SuMon/openapi-server.json`

M2 阶段会引入 `openapi-typescript` 自动从 JSON 生成 DTO 类型。

## 已实现 / 未实现

| 功能 | 状态 | 里程碑 |
|---|---|---|
| 工程骨架、Vite 代理、ESLint、Prettier | ✅ | M1 |
| Dashboard 占位（健康/就绪/服务器数探测） | ✅ | M1 |
| 404 视图 | ✅ | M1 |
| 登录、注册、当前用户、退出 | ⏳ | M2 |
| 路由守卫与权限控制 | ⏳ | M2 |
| 主布局（顶栏 + 侧栏） | ⏳ | M3 |
| 真实 Dashboard 卡片 | ⏳ | M3 |
| 服务器列表/详情/创建/编辑/软删除 | ⏳ | M4-M6 |
| 管理员审核 | ⏳ | M7 |
| SSH 终端、实时指标、告警页面 | ❌ | 后端就绪后另立 |

## 调试入口

- 健康检查：浏览器 DevTools → Network → `/api/health` 应见 `code=0`
- 就绪检查：`/api/ready` 应见 `code=0`、`database=ok`
- 服务器列表：`/api/servers` 无 Token 时返回 `code=40100`（预期）

## 已知约束

- 仅依赖已实现的后端接口；SSH 连接测试、实时指标、告警、终端等功能
  的前端入口暂不渲染，对应后端能力就绪后单独开发。
- 未引入单元/E2E 测试框架，待 MVP-5 收口时统一补齐。
- `package-lock.json` 在执行 `npm install` 后生成，需提交至版本控制。

## 前端开发

### 快速开始

```powershell
Set-Location "C:\Users\genhaosan\Desktop\SuSuMonitor(Jvav)\web-vue-SuMon"
npm install
npm run dev    # http://127.0.0.1:5173,自动代理 /api → :18080
```

### 当前已实现功能(2026-07-21 更新)

| 功能 | 状态 | 关联 |
|---|---|---|
| 工程骨架、Vite 代理、ESLint、Prettier | ✅ | M1 |
| 登录/注册/当前用户/退出 | ✅ | M2 |
| 路由守卫(requiresAuth/requiresAdmin/publicOnly) | ✅ | M2 + 启动兜底 |
| MainLayout(顶栏 + 侧栏 + 用户下拉) | ✅ | M3 |
| 涂山苏苏主题视觉(玻璃卡 + 铃铛花瓣波浪 + 签名引言) | ✅ | M3-M4 |
| Dashboard 美化(玻璃卡 + 渐变徽标 + 数字滚动 + 头像) | ✅ | 后续打磨 |
| 服务器列表/详情/创建/编辑/软删除(客户端排序) | ✅ | M4 |
| 管理员审核页(通过/拒绝 + 错误码映射) | ✅ | M5 |
| 路由切换 loading bar(NProgress) | ✅ | UX 打磨 |
| M6 实时监控页(ticket + WS + REST 历史 + 卡片 + 表) | ✅ | M6 |
| OpenAPI 契约 lint 脚本 | ✅ | 自动化 |
| pre-commit 钩子(跑 openapi:check) | ✅ | 自动化 |
| 服务器列表 spark line | ⚠️ mock | 等待 metrics history 接口 |
| 上次 SSH 测试结果卡 | ⚠️ 占位 | 等待 SSH test history 接口 |
| 批量审核/用户搜索/历史记录 | ❌ | 后端无对应接口 |

### 扩展命令

| 命令 | 作用 |
|---|---|
| `npm run dev` | 开发服务器(端口 5173) |
| `npm run build` | 生产构建(类型检查 + 打包) |
| `npm run typecheck` | TypeScript 检查 |
| `npm run lint` / `lint:fix` | ESLint |
| `npm run format` | Prettier 格式化 |
| `npm run openapi:check` | OpenAPI 契约 lint |

### 目录结构(2026-07-21 更新)

```text
web-vue-SuMon/
├── .gitignore
├── .eslintrc.cjs
├── .prettierrc.json
├── package.json
├── tsconfig.json
├── tsconfig.node.json
├── vite.config.ts
├── index.html
├── env.d.ts
├── README.md
├── scripts/
│   ├── check-openapi.mjs       # OpenAPI 契约结构 lint
│   └── README.md
├── public/
│   ├── favicon.jpg              # 涂山苏苏 favicon
│   └── tushansusu-hero.jpg      # LoginLayout 兜底图
├── .husky/
│   └── pre-commit               # 跑 npm run openapi:check
└── src/
    ├── main.ts
    ├── App.vue
    ├── styles/global.css
    ├── api/                     # HTTP 客户端封装
    ├── stores/                  # Pinia 状态(auth 等)
    ├── router/
    │   ├── index.ts             # 路由表 + 守卫 + 进度条
    │   └── guards.ts            # 鉴权守卫
    ├── layouts/                 # MainLayout
    ├── components/              # 公共组件(PageHeader/TushanFoxMark/ServerFormDialog)
    ├── composables/             # 组合式函数(useRouterLoading)
    ├── views/                   # 页面组件
    ├── utils/                   # 工具函数
    └── types/                   # TypeScript 类型(对齐 OpenAPI)
```

### pre-commit 钩子

`web-vue-SuMon/.husky/pre-commit` 在每次 `git commit` 前自动跑 `npm run openapi:check`,
失败则 commit 被阻止。

首次 clone 后手动激活(在仓库根执行):

```bash
git config core.hooksPath web-vue-SuMon/.husky
```

跳过:`git commit --no-verify`(不推荐)。

### 重要:涂山 IP 使用范围

本项目使用涂山苏苏等第三方 IP 形象,**仅供内部学习与 demo 用途,非官方同人作品,不用于商业用途**。
公开展示或商业化前请替换为自有素材或已获授权的版本。
详见各页面引言池、`/docs-SuMon/Bug-fix/` 目录与各 dev-log。

### 关键设计决策

| 决策 | 方案 | 理由 |
|---|---|---|
| 路由切换视觉反馈 | NProgress 顶部条 | 零状态管理,3KB,主题可定制 |
| Dashboard 数字滚动 | `requestAnimationFrame` + easeOutCubic | 平滑,无依赖,1.2s |
| 服务器列表排序 | 客户端 computed 排序 | 后端 `sort_by` 暂被忽略(MVP-4 bug),见 `docs-SuMon/Bug-fix/` |
| 鉴权 token 存储 | localStorage via pinia-plugin-persistedstate | 刷新保留,可被"记住我"控制 |
| 错误码映射 | `src/types/error-code.ts` 集中常量,各 view 各自映射 toast | 单一事实源,后端改码只改一处 |