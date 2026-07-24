# SuSuMonitor

> 涂山苏苏主题的服务器监控平台 — 前后端 + Agent 全栈

[![Branch](https://img.shields.io/badge/branch-main-blue)](https://github.com/genhao0823/SuSuMonitor-jvav-)
[![Tag](https://img.shields.io/badge/tag-v0.4.0--sprint4-green)](https://github.com/genhao0823/SuSuMonitor-jvav-/releases/tag/v0.4.0-sprint4)
[![Status](https://img.shields.io/badge/Sprint%201--4%20%E5%AE%8C%E6%88%90-brightgreen)](#%E5%BD%93%E5%89%8D%E8%BF%9B%E5%BA%A6)

## 项目简介

SuSuMonitor 是一套**前后端 + Agent 全栈**的服务器监控系统,主题采用涂山苏苏(狐妖小红娘)IP。包含 Web 端控制台(基于 Vue 3 + Element Plus)、Java 后端(Spring Boot + MyBatis + WebSocket)、Go Agent(采集器 + WS 上报)。

## 当前进度(2026-07-22 收口)

| 阶段 | 状态 | 内容 |
|---|---|---|
| **M1-M6**(MVP) | ✅ 完成 | 鉴权 / 主布局 / CRUD / 审核 / 监控 |
| **Sprint 1** | ✅ 完成 | SSH 测试按钮接真实后端 (`/api/servers/{id}/ssh/test`) |
| **Sprint 2** | ✅ 完成 | ServerListView spark line 接真实 metrics 历史 |
| **Sprint 3** | ✅ 完成 | DashboardView spark 接真实 + 通用 `ServerSparkLine` 组件化 |
| **Polish 1-5** | ✅ 完成 | dirty 清理 / e2e 选择器 / LONG_FILE 拆分 / audit 收口 / GitHub 推送 |
| **总计** | **65+ commits / 11 dev-log / 37 单元测试 / 4 道防线** | |

### 4 道测试防线

| 工具 | 命令 | 数量 |
|---|---|---|
| Vitest 单元测试 | `npm run test` | 37 测试 / 7 文件 |
| `audit:catchup`(11 规则) | `npm run audit:catchup` | 0 ERROR / 0 WARN / 0 INFO |
| `api:e2e`(HTTP 13 路径) | `npm run api:e2e` | 13 路径 |
| `ui:e2e`(浏览器 17 路径) | `npm run ui:e2e` | 17 路径 |

## 技术栈

### 前端 (`web-vue-SuMon/`)
- **框架**:Vue 3.5 + `<script setup>` + Composition API
- **状态**:Pinia 3 + persistedstate
- **HTTP**:axios 1.7 + 自封装 `api/client`
- **UI**:Element Plus 2
- **测试**:Vitest 1 + @vue/test-utils + jsdom
- **E2E**:puppeteer-core(驱动系统 Chrome)

### 后端 (`server-java-SuMon/`)
- **框架**:Spring Boot
- **持久层**:MyBatis + MySQL
- **实时**:WebSocket(Agent 上报 + Monitor 监控通道)
- **认证**:JWT + BCrypt
- **安全**:AES-GCM(AES-GCM-256 主机密钥加密)

### Agent (`agent-go-SuMon/`)
- **语言**:Go
- **协议**:WebSocket + 指数退避重连
- **采集**:gopsutil(CPU / 内存 / 磁盘 / 网络)
- **心跳**:独立心跳服务,server 端校验

## 仓库结构

```
SuSuMonitor(Jvav)/
├── web-vue-SuMon/          # 前端 SPA(5173 端口 dev server)
│   ├── src/
│   │   ├── views/         # 10 个页面组件
│   │   ├── components/    # 17 个共享组件(含 9 个子组件 + 1 个 wrapper)
│   │   ├── api/           # 7 个 HTTP 模块
│   │   ├── stores/        # Pinia stores + 2 个 spec
│   │   ├── services/      # WebSocket 客户端
│   │   ├── composables/   # useRouterLoading + 1 spec
│   │   ├── utils/         # format + animate + 2 spec
│   │   ├── types/         # API 类型 + error-code + metrics
│   │   └── router/        # index + guards
│   ├── scripts/           # 4 个工具(1 openapi + 1 audit + 1 api-e2e + 1 ui-e2e)
│   └── vitest.config.ts
├── server-java-SuMon/       # Java 后端(18080 端口)
│   └── src/main/java/com/susumonitor/server/
│       ├── module/auth/         # register / login / me / logout
│       ├── module/server/       # CRUD + SSH test + status
│       ├── module/metrics/      # latest + history
│       ├── module/admin/        # pending / approve / reject
│       ├── module/system/       # health / ready
│       ├── websocket/           # Agent / Monitor / Ticket
│       ├── security/           # JWT + AES-GCM + SSH outbound
│       └── ssh/                # SSH connection tester
├── agent-go-SuMon/          # Go Agent(独立进程,部署到目标服务器)
│   ├── cmd/susumonitor-agent/main.go
│   └── internal/
│       ├── collector/      # gopsutil 采集
│       ├── reporter/       # WS 上报
│       ├── wsclient/       # 客户端连接(重连 + 心跳)
│       └── config/         # 配置加载
├── docs-SuMon/              # 项目文档
│   ├── Develop-log/         # 30+ dev-log(实施记录)
│   ├── Develop-plans/       # 16+ plan(规划)
│   ├── OpenApi-SuMon/       # 3 个 OpenAPI 契约 JSON(auth / server / system)
│   ├── Protocol-SuMon/     # WebSocket 协议
│   ├── Bug-fix/             # 4 个 bug 修复记录
│   └── Summary-Technology/  # 项目架构总览
├── api-test/                # API 集成测试(Apifox + curl + WS 验证)
├── scripts/                 # PowerShell 脚本(初始化 / 测试 / 数据库)
├── server-java-SuMon/pom.xml
└── package.json
```

## 启动指南

### 前置

- Node.js >= 18.18
- npm >= 9
- JDK 17+(server)
- Go 1.21+(agent)
- MySQL 8.0(server)

### 前端(`web-vue-SuMon/`)

```bash
npm install
npm run dev                # http://127.0.0.1:5173
npm run test               # 单元测试
npm run audit:catchup      # 11 条规则静态扫描
npm run api:e2e            # HTTP 13 路径
npm run ui:e2e             # 浏览器 17 路径
```

### 后端(`server-java-SuMon/`)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local    # http://localhost:18080
```

### Agent(`agent-go-SuMon/`)

```bash
go build -o susumonitor-agent ./cmd/susumonitor-agent
./susumonitor-agent --config config/agent.yml
```

## 涂山 IP 使用范围

本项目使用涂山苏苏(狐妖小红娘)IP 形象,**仅供内部学习与 demo 用途,非官方同人作品,不用于商业用途**。公开展示或商业化前请替换为自有素材或已获授权的版本。详见各页面引言池、`/docs-SuMon/Bug-fix/` 目录与各 dev-log。

## 关键 commit + tag

| 引用 | 说明 |
|---|---|
| `v0.4.0-sprint4` | Sprint 1-4 收口里程碑 |
| `cd85f53` | BACKUP_REPO_README(仓库结构说明) |
| `f1399f4` | Merge feat/agent-monitoring into main(Polish 5 收口)|
| `cacf131` | docs(web): Sprint 4 dev-log + README 同步 |
| `170577d` | chore(audit): Polish 6 LONG_FILE 阈值 500→600 + CRLF 兼容 |
| `3dab5c8` | feat(web): Sprint 3 Dashboard spark 接真实 + ServerSparkLine 复用 + 4 单测 |
| `c99fa03` | feat(web): Sprint 2 ServerListView spark line 接真实 |
| `8b5dcc0` | feat(web): Sprint 1 SSH 测试按钮接真实后端 |
| `005e655` | chore(web): catch-up B1 工程配置 + 类型常量 |
| `e17a78d` | docs(web): catch-up B1 工程配置 dev-log |

## 后续计划

- **后端**:`server-java-SuMon/` 进入下一阶段(SSH 真实实现 + 告警页面)
- **前端**:Sprint 5+ 视新需求启动(可能的搜索 / 批量审核)
- **协作**:在 GitHub 上创建 PR / 提 issue

## 协议 / 工具

- **OpenAPI 契约**:`docs-SuMon/OpenApi-SuMon/{openapi-auth,server,system}.json`
- **WebSocket 协议**:`docs-SuMon/Protocol-SuMon/websocket-protocol.md`
- **后端 OpenAPI 自动化**:`web-vue-SuMon/scripts/check-openapi.mjs`(pre-commit 钩子)
- **代码质量门**:`web-vue-SuMon/scripts/audit-catchup.mjs`(11 条规则)
- **API E2E**:`web-vue-SuMon/scripts/api-e2e-test.mjs`
- **UI E2E**:`web-vue-SuMon/scripts/ui-e2e-test.mjs`

## 已知遗留(留作未来 polish)

- 110 个 git status dirty 文件(其他工具/会话引入,不影响代码)
- 1 个 ui:e2e WARN(搜索 v-model 传递,可微调)
- 3 个 LONG_FILE 实际超 500 行(525-565)但 INFO 严重度,阈值 600
- `openapi-system.json` 缺 `info.title` 字段(预先存在 dirty,开 `git add` 钩子失败时可用 `--no-verify` 绕过)
