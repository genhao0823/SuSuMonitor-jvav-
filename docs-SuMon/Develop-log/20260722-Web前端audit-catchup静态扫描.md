# 开发日志: catch-up 静态审计脚本(audit-catchup.mjs)

**日期**: 2026-07-22
**操作人**: opencode
**关联计划**: M6 验收 + Module 1(我自己的 round-summary 改良建议)
**关联需求**: catch-up 大批 commit 前后自动捕获 page_size / row 134 类隐藏 bug

## 状态

`web-vue-SuMon/scripts/audit-catchup.mjs` 落地,`npm run audit:catchup` 可跑。
当前 src/ 36 个文件全部通过 6 条规则(0 ERROR / 0 WARN / 0 INFO,exit 0)。
此工具是 Module 2(M2-M5 回归)的扫描器前置依赖。

## 本次落地的工程文件

### 新增文件(1)

- `web-vue-SuMon/scripts/audit-catchup.mjs`(206 行)
  - 6 条审计规则:
    1. `MAGIC_PAGE_SIZE`(ERROR): `page_size: NNN` 硬编码 > 100
    2. `PARAM_NAME_MISMATCH`(ERROR): `params.id` 用在 `*.vue` 路由跳转
    3. `ROUTE_PARAMS_ID`(ERROR): `route.params.id` 读取
    4. `API_PATH_TYPO`(WARN): `apiClient.*` 路径不在 OpenAPI 中
    5. `HARDCODED_HOST_PORT`(INFO): src/ 硬编码 host:port
    6. `TODO_FETCHER`(INFO): `// TODO` 残留
  - 输出风格完全对齐 `check-openapi.mjs`(✓/✗/⚠/退出码 0/1)
  - 零依赖:仅用 Node 内置 `node:fs` / `node:path` / `node:url`

### 修改文件(2)

- `web-vue-SuMon/package.json`
  - 新增 script: `"audit:catchup": "node scripts/audit-catchup.mjs"`

- `web-vue-SuMon/scripts/README.md`
  - 新增"## audit-catchup.mjs"段(~79 行)
  - 含用法 / 6 条规则说明 / 输出示例 / 设计原则 / 与 openapi:check 的对比 / 故意不做的事

## 关键设计要点

- **零依赖**:与 `check-openapi.mjs` 风格保持一致,纯 Node 内置模块
- **纯只读**:不改任何源文件,CI 友好
- **退出码语义**:0 全过 / 1 有 ERROR / 2 脚本异常,可被 `&&` 串接
- **错误分级**:ERROR 拦截(commit 前必须修)/ WARN 提示(建议修)/ INFO 留痕(可选修)
- **故意不做 schema 字段交叉比对**:OPENAPI_SCHEMA_DRIFT 规则 noise > signal,在第一次跑时删掉;Module 2 视需求再设计更复杂版本(避免陷入提前优化陷阱)

## 验证清单

| 检查 | 命令 | 结果 |
|---|---|---|
| 自运行 | `npm run audit:catchup` | 0/0/0 ✅ |
| typecheck | `npm run typecheck` | 0 错误 ✅ |
| lint | `npm run lint` | 0 错误 0 警告 ✅ |
| OpenAPI | `npm run openapi:check` | 3/3 通过 ✅ |
| pre-commit hook | 自动跑 | 通过 ✅ |
| dev server | 5173 端口 | 持续 LISTEN ✅ |

## 工具迭代记录

### v0.1(本次)

- 初版 7 条规则,跑出 30+ false positive
- 修复 1:`MAGIC_PAGE_SIZE` filter `>=` → `>`(避免 100 也被报)
- 修复 2:删 `OPENAPI_SCHEMA_DRIFT`(noise > signal)
- 最终 6 条规则,src/ 全过

### 未来 v0.2 候选

- Module 2 跑回归时若发现未覆盖的 bug → 加新规则
- `API_PATH_TYPO` 可升级为读 OpenAPI JSON 的 `parameters` 节点,做"参数类型"比对
- 加 `MAX_PAGE_SIZE` 常量从 OpenAPI schema 读,而不是硬编码 100

## 与已有工具的关系

| 工具 | 范围 | 检查项 |
|---|---|---|
| `openapi:check` | OpenAPI JSON 文件 | 结构性 lint(顶层 / version / title / paths) |
| `audit:catchup` | 前端 src/ TS/Vue | 业务 lint(魔法数字 / 参数名 / API 路径 / 硬编码) |
| `eslint` | 全部 .ts / .vue | 代码风格 / 未使用变量 |
| `vue-tsc` | 全部 .ts / .vue | TypeScript 类型推导 |

四套互补,组成"结构 + 业务 + 风格 + 类型"四视角校验。

## 风险与对策

| 风险 | 处理 |
|---|---|
| regex 规则 false positive 再次出现 | 故意从 7 条降到 6 条,留出更多判断空间;Module 2 跑回归时再补 |
| 工具被 bypass | 当前不在 pre-commit hook 里(M2-M5 跑回归时再加),避免误拦 |
| 误删(false positive 修代码时) | Module 3 把"Edit 后必须 `git diff` 自检"沉淀进 karpathy-guidelines skill |

## 后续

- Module 2:M2-M5 回归 + bug 修复(用本工具扫描)
- Module 3:Edit 误删防护写全局 skill
- Module 4:git push 决策 + 执行

## Commit

- `56812d2 chore(web): scripts/audit-catchup.mjs 静态审计 catch-up 常见 6 类 bug`(本工具落地)
- 本 dev-log 同步在 docs commit(预计紧随 commit 1 后)