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