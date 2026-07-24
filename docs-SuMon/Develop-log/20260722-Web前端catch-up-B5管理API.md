# 开发日志: 前端 catch-up(B5) — 管理 API + utils 工具

**日期**: 2026-07-22
**操作人**: opencode
**关联计划**: 历史 catch-up
**关联需求**: M5 管理 API 支撑 + 跨 view 复用的格式化/动画工具

## 状态

`api/admin.ts`、`utils/format.ts`、`utils/animate.ts` 三文件已落库。
配合 `8b100b5 feat(m5): 用户审核页(AdminUsersView)` 现有的 commit,M5 完整闭环。

## 本次落地的工程文件

### 新增文件(3 个 / 200 行)

| 文件 | 行数 | 说明 |
|---|---|---|
| `web-vue-SuMon/src/api/admin.ts` | 42 | `listPendingUsers` / `approveUser` / `rejectUser` 3 个函数 |
| `web-vue-SuMon/src/utils/format.ts` | 95 | `formatDateTime` / `serverStatusTagType` / `serverStatusLabel` / `userRoleLabel` / `reviewStatusLabel` / `reviewStatusTagType` |
| `web-vue-SuMon/src/utils/animate.ts` | 63 | `animateCounter`(requestAnimationFrame + easeOutCubic) |

## 关键设计要点

- **`formatDateTime` 全统一**:任何 view 都不自己实现时间格式化,降低风格漂移。
- **`serverStatusTagType` 与 view 解耦**:view 拿到枚举 → `format.ts` 映射 → el-tag type。
  后端改枚举值,只改 `format.ts` 一处。
- **`animateCounter` 单一事实源**:Dashboard 数字滚动复用此函数,不重复实现 RAF 循环。
- **`api/admin.ts` 与 `8b100b5` 配合**:`AdminUsersView.vue` 已 commit,本次仅补 API 支撑,
  避免 commit 颗粒度过小或过大。

## 后续

- B 全部 catch-up 完成,接下来统一跑 typecheck / lint
- A: M6 接通 + 修 ServerDetailView 路由参数 bug
