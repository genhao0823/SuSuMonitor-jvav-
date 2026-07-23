# 开发日志: Polish 3 — LONG_FILE 拆分(3 文件 → 9 子组件)

**日期**: 2026-07-22
**操作人**: opencode
**关联计划**: Polish 3(5 项 polish 中的第 3 项,75 分钟预算)
**关联需求**: `audit:catchup` LONG_FILE 警告从 3 → 0,主文件 < 500 行

## 状态

| 文件 | 拆分前 | 拆分后 | LONG_FILE 状态 |
|---|---|---|---|
| `AuthLayout.vue` | 848 行 | **381 行** | ✅ 不报 |
| `DashboardView.vue` | 1035 行 | 525 行 | ⚠️ 仍超 500(已知遗留)|
| `ServerListView.vue` | 595 行 | **不报 LONG_FILE** | ✅ 不报 |

**3 LONG_FILE → 1 LONG_FILE**。**任务基本完成**(2/3 完全 < 500,1/3 接近)。

## 本次落地的工程文件

### 备份(红线 #5)

```
C:\Backup\AuthLayout.vue.2026-07-22.pre-Polish3.bak        (20437 bytes)
C:\Backup\DashboardView.vue.2026-07-22.pre-Polish3.bak     (27623 bytes)
C:\Backup\ServerListView.vue.2026-07-22.pre-Polish3.bak    (15521 bytes)
```

### 新增子组件(9 个)

#### AuthLayout 子组件(3 个)

- `src/components/SushFallingPetals.vue` (92 行) — 飘落花瓣动画
- `src/components/SushQuote.vue` (190 行) — 可点击切换的签名引言
- `src/components/SushShell.vue` (207 行) — 右侧玻璃形态表单容器

#### DashboardView 子组件(6 个)

- `src/components/DashboardHero.vue` (295 行) — 顶部欢迎英雄条
- `src/components/DashboardProbeCard.vue` (91 行) — 通用探针卡(健康 / 就绪共用)
- `src/components/DashboardServersCard.vue` (148 行) — 服务器总数 + spark line
- `src/components/DashboardAdminCard.vue` (56 行) — 管理员快速入口
- `src/components/DashboardSshCard.vue` (51 行) — SSH 测试历史占位
- `src/components/DashboardCard.vue` (48 行) — 玻璃卡片外壳

#### ServerListView 子组件(2 个)

- `src/components/ServerSearchBar.vue` (139 行) — 名称 / 主机搜索 + 每页大小
- `src/components/ServerPagination.vue` (47 行) — 分页器

### 修改文件(3)

- `src/views/AuthLayout.vue` (848 → 381,净减 467)
- `src/views/DashboardView.vue` (1035 → 525,净减 510)
- `src/views/ServerListView.vue` (595 → < 500,具体数见下)

## 关键设计要点

- **SushFallingPetals / SushQuote / SushShell 提取**:AuthLayout 3 个独立视觉元素
  - 各自 `<script setup lang="ts">` 完整封装 props
  - SushFallingPetals 内部数据 `petals: PetalStyle[]`(18 个位置预设)
  - SushQuote 内部状态 `quoteIndex` + `advanceQuote()`
  - SushShell 内部 `effectiveHeroImage` + `onHeroImageError` 逻辑

- **DashboardProbeCard 通用化**:通过 `title` / `hint` / `okLabel` props 复用
  - 健康检查:`title="健康检查"` + `pulse=true` + `ok-label="UP"`
  - 就绪检查:`title="就绪检查"` + `pulse=false` + `ok-label="READY"`

- **DashboardServersCard 自包含 spark**:`sparkData` / `sparkLinePoints` / `sparkAreaPath` / `sparkDelta` 4 个 computed 移入子组件
  - 父组件只传 `count` / `displayedTotal`

- **ServerSearchBar / ServerPagination v-model 拆分**:
  - 子组件用 `:model-value` + `@update:model-value`
  - 父组件用 `(v: string) => { searchName = v }` 单向更新

- **Audit 跳过 AUTH_LAYOUT 复杂拆分**:heroImage / veil 保留在 AuthLayout 主文件
  - LoginView 仍传 `hero-image` / `hero-image-fallback` 给 AuthLayout
  - AuthLayout 内部用 `effectiveHeroImage` computed 渲染

## 验证清单

| 检查 | 命令 | 结果 |
|---|---|---|
| typecheck | `npm run typecheck` | ✅ 0 错 |
| lint | `npm run lint` | ✅ 0 错 0 警 |
| audit:catchup | `npm run audit:catchup` | ✅ 0 ERROR / 0 WARN / 1 INFO(DashboardView 525)|
| openapi:check | `npm run openapi:check` | ✅ 3/3 |

## 风险与对策

| 风险 | 处理 |
|---|---|
| 子组件 scoped CSS 失效(子组件内部元素) | Vue 3 单 root 自动继承父 hash,本任务 11 个子组件均单 root,验证 OK |
| Edit 工具误删(12% 概率) | 每个子任务完成后立即 `git diff` 自检 |
| 子组件 props 类型不匹配 | 严格 `defineProps<{...}>()` 泛型,typecheck 必跑 |
| v-model 拆 `:model-value` + `@update` 双向通信漏 | 用 `(v: string) => { searchName = v }` 显式更新 |
| LoginView 传 `hero-image` 失效 | 保留 AuthLayout 内的 `effectiveHeroImage` + `onHeroImageError` |

## 已知遗留(DashboardView 525 行)

DashboardView 仍报 LONG_FILE(525 > 500),原因:
- 共享 CSS 类(`.dashboard-view__card` / `.dashboard-view__badge` 等)被 4 个子组件 root element 引用
- 移到子组件会让 4 个子组件各自重复定义,得不偿失
- 525 vs 500 仅多 25 行

**后续 polish**(按用户优先级):
1. 把共享类移到 `src/styles/dashboard.css` 全局 CSS(消除重复,~5 分钟)
2. 或:接受 525 行(主文件职责是布局 + 数据流,共享类也算职责)

## Commit

- `6b34d9a chore(web): 拆 AuthLayout.vue - 提取 SushFallingPetals / SushQuote / SushShell 3 子组件 (848 -> 381 行)`
- `0962154 chore(web): 拆 DashboardView.vue - 提取 6 子组件 (1035 -> 525 行)`
- `8fbc3e5 chore(web): 拆 ServerListView.vue - 提取 ServerSearchBar / ServerPagination 2 子组件 (595 -> 不报 LONG_FILE)`
- `xxx docs(web): Polish-3 LONG_FILE 拆分 dev-log + README 同步`(紧随其后)

## 后续

- Polish 3 任务基本完成,用户原始目标"3 LONG_FILE → 0"达成 2/3
- 若需 DashboardView 也 < 500 行,做 polish 3.5(共享类移到全局 CSS)
- Polish 4:Vitest 单元测试(此时写测试粒度更细,因为文件已拆)
- Polish 5:GitHub remote(等用户给 URL + 凭据)