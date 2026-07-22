# scripts/

自动化与校验脚本。

## check-openapi.mjs

OpenAPI JSON 结构 lint 脚本。

### 用法

```bash
npm run openapi:check
```

扫描 `docs-SuMon/OpenApi-SuMon/*.json`,对每份契约做 4 项最低线校验:

1. 顶层必含 `openapi` / `info` / `paths`
2. `openapi` 版本必须是 `3.0.x`
3. `info.title` 与 `info.version` 必须非空字符串
4. `paths` 至少含一条 HTTP 端点(get/post/put/delete/patch/options/head)

### 设计原则

- 纯只读:**不会修改任何 JSON 文件**
- 离线:**不依赖网络**,纯本地文件 IO
- 零依赖:仅用 Node 内置 `node:fs` / `node:path`,不需要 `npm install`
- CI 友好:退出码 `0` 全通过,`1` 有失败

### 输出示例(成功)

```
OpenAPI 契约结构校验
================================

✓ openapi-auth.json     (8 endpoints)
✓ openapi-server.json   (15 endpoints)
✓ openapi-system.json   (3 endpoints)

================================
3 / 3 通过
```

### 输出示例(失败)

```
OpenAPI 契约结构校验
================================

✓ openapi-auth.json     (8 endpoints)
✗ openapi-system.json   失败:
    - info.title 缺失或为空
✓ openapi-server.json   (15 endpoints)

================================
2 / 3 通过
```

### 失败处理

修复契约 JSON → 重新执行 `npm run openapi:check`。
脚本不修改源文件,所有修复由人/后端工具完成。

### 故意不做的事

- **不校验 schemas / components**:业务契约由后端维护
- **不校验路径格式**:路径风格是 OpenAPI 设计问题,不在 lint 范围
- **不校验跨文件一致性**:三份 JSON 分属不同模块,跨文件校验通过 Apifox import 验证
- **不校验中文/特殊字符**:OpenAPI 3 允许 Unicode

---

## pre-commit 钩子

`web-vue-SuMon/.husky/pre-commit` 会在每次 `git commit` 前自动跑 `npm run openapi:check`。
校验失败时 git 拒绝本次 commit。

### 钩子配置

- `core.hooksPath = web-vue-SuMon/.husky`（仓库根相对路径）
- 钩子脚本会 `cd` 到 `web-vue-SuMon/` 后跑校验
- 钩子的退出码透传给 git:非 0 时 commit 被中止

### 绕过钩子

紧急情况可用 `git commit --no-verify -m "..."` 跳过钩子,**不推荐**。

### 团队成员上手

**当前实现**：钩子通过手动 `git config core.hooksPath` 配置,不依赖 husky 包。
若在新机器上 clone 仓库,需要在仓库根跑一次:

```bash
git config core.hooksPath web-vue-SuMon/.husky
```

后续如果引入 husky 包(团队扩大、CI 多端),可加 `"prepare": "husky"` script 到 package.json 实现自动配置。
但当前不需要 husky 作为 devDep,手动 git config 即可。

### 关于 husky 包

我最初尝试用 husky 9.1.7 实现,遇到 Windows + 父子目录 `.git` 的兼容问题
(husky 在 web-vue-SuMon/ 跑找不到父级 .git)。
**当前方案放弃 husky,改为手写 shim**,更可控、更轻量、零依赖。

---

## audit-catchup.mjs

catch-up 流程静态审计脚本。**在 catch-up 大批 commit 前后各跑一次**,
捕获魔法数字 / 参数名错误 / API 路径拼错等常见 bug。

### 用法

```bash
npm run audit:catchup
```

扫描 `web-vue-SuMon/src/**/*.vue` / `*.ts`,对每份文件做 6 项规则审计:

| 规则 ID | 严重度 | 检测内容 |
|---|---|---|
| `MAGIC_PAGE_SIZE` | ERROR | `page_size: NNN` 硬编码 > 100 |
| `PARAM_NAME_MISMATCH` | ERROR | router-link 用 `params.id`(路由是 `:serverId`) |
| `ROUTE_PARAMS_ID` | ERROR | `route.params.id` 与路由 path 不匹配 |
| `API_PATH_TYPO` | WARN | `apiClient.*` 路径不在 OpenAPI 中 |
| `HARDCODED_HOST_PORT` | INFO | src/ 硬编码 host:port |
| `TODO_FETCHER` | INFO | `// TODO` 残留 |

### 输出示例(成功)

```
Catch-up 静态审计
================================
扫描 36 个 .vue / .ts 文件...

✓ 所有文件通过 6 条审计规则

================================
0 ERROR / 0 WARN / 0 INFO
```

### 输出示例(失败)

```
✗ [MAGIC_PAGE_SIZE] web-vue-SuMon/src/views/ServerListView.vue:305 page_size: 999 硬编码
⚠ [API_PATH_TYPO] web-vue-SuMon/src/api/auth.ts:20 /api/login — 不在 OpenAPI 中
================================
1 ERROR / 1 WARN
```

退出码:`0` 全通过 / `1` 有 ERROR / `2` 脚本执行异常。

### 设计原则

- 纯只读(不修改任何源文件)
- 离线(不依赖网络)
- 零依赖(仅用 Node 内置模块)
- CI 友好(退出码可被 `&&` 串接)

### 何时跑

- **catch-up 大批 commit 之前** — 提前发现风险
- **catch-up 大批 commit 之后** — 验证没遗漏
- **PR 提交前** — 防止新代码引入同类 bug
- **回归测试期间** — Module 2 M2-M5 回归前必跑

### 与 openapi:check 的关系

| 工具 | 检查目标 | 检查内容 |
|---|---|---|
| `openapi:check` | OpenAPI JSON 文件 | 结构(顶层、version、title、paths) |
| `audit:catchup` | 前端 TS/Vue 文件 | 业务(魔法数字、参数名、API 路径、硬编码 host) |

两套互补,共同组成 "API 契约 + 前端用法" 双视角校验。

### 故意不做的事

- **不做完整 OpenAPI schema 字段交叉比对**(需要解析 properties 树,Module 2 视需求决定是否补)
- **不做 TS 类型推导对比**(需要解析 d.ts,留给 eslint-plugin-typescript)
- **不扫 .vue `<script>` 外的模板**(避免 false positive)