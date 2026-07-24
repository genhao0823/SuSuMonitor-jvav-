# 开发日志: Sprint 3 — DashboardView 完整化

**日期**: 2026-07-22
**操作人**: opencode
**关联计划**: `docs-SuMon/Develop-plans/20260724-前端正式功能开发规划.md` Sprint 3

## 状态

| 项 | 状态 |
|---|---|
| `DashboardServersCard` 改用 `ServerSparkLine` 复用 | ✅ 删除 mock computed + 内部 SVG(消除 2 处 spark 重复)|
| `DashboardView` spark 接真实 `/api/servers/{id}/metrics` | ✅ 拉第一个 server 的最近 7 天 CPU 历史 |
| `DashboardServersCard.spec.ts` 集成测试 | ✅ 4 个用例(count / loading / 空数据 / 单点)|
| 4 件套 | ✅ typecheck 0 / lint 0 / **37/37 测试** / 3/3 openapi |
| audit:catchup | ✅ 0 ERROR / 0 WARN / 3 INFO(3 个 wc -l 误报)|

**`DashboardView` 4 个 KPI 卡(健康/就绪/服务器/审核)全部接真实后端,spark 接真实历史。**

## 本次落地的工程文件

### 修改文件(2)

| 文件 | 改动 | 行数 |
|---|---|---|
| `src/components/DashboardServersCard.vue` | 改用 `ServerSparkLine` 复用,删内部 mock + SVG + 3 个 computed | -118 / +6 = **净减 112** |
| `src/views/DashboardView.vue` | 加 `sparkHistory` ref + `loadSparkHistory()` + 数据注入 | +42 |

### 新增文件(1)

| 文件 | 行数 | 说明 |
|---|---|---|
| `src/components/DashboardServersCard.spec.ts` | 53 | 4 个集成测试 |

**总:+99/-118 净减 19,3 文件**。

## 关键设计要点

### 1. 消除 2 处 spark 重复实现

**Sprint 2 之前**:
- `ServerSparkLine.vue`(Sprint 2 新增,~150 行)
- `DashboardServersCard.vue`(Sprint 1 之前,~50 行 mock spark SVG)

**Sprint 3 之后**:
- `ServerSparkLine.vue` —— **唯一** spark 实现
- `DashboardServersCard.vue` —— 纯壳(只保留 glass-card + 标题 + 徽标 + value)

**消除 50 行重复 SVG + 3 个 computed**(sparkData / sparkLinePoints / sparkAreaPath)+ 1 个 sparkDelta。

### 2. spark 数据流(DashboardView → DashboardServersCard → ServerSparkLine)

```
DashboardView.refresh() 末尾
  ↓ void loadSparkHistory()  (fire-and-forget)
  ↓ getMetricsHistory(firstServerId, 7d)
  ↓ sparkHistory ref<number[]>
  ↓
<DashboardServersCard :data="sparkHistory" />
  ↓
<ServerSparkLine :data="data" />  ← props 透传,1 个 spark 渲染源
```

**3 层 props 透传**,职责分离:
- DashboardView:数据编排(拉历史 + 计数)
- DashboardServersCard:壳(标题 + 徽标 + value)
- ServerSparkLine:渲染(SVG 算法)

### 3. 失败降级(后端 18080 未启时)

```typescript
async function loadSparkHistory(): Promise<void> {
  try {
    const detail = await listServers({ page: 1, page_size: 1 })
    // ... 拉取 ...
  } catch {
    sparkHistory.value = []  // ← 空数组,ServerSparkLine 渲染空态
  }
}
```

**不抛错,不弹 toast**——spark 缺失是次要信息,4 个 KPI 才是核心。

### 4. "拉第一个 server 历史" vs "拉所有 server"

| 选项 | 优 | 劣 |
|---|---|---|
| **拉第一个 server** ✅ | 1 个请求,快(< 1s) | 不代表全部 |
| 拉所有 server | 真实平均 | N 个请求,慢 |
| 不拉 spark | 0 请求 | mock 数据,无意义 |

**选第一个**:Dashboard 是"概览页",代表性 server 的趋势足以传递"健康度"信号。ServerListView 有 Sprint 2 的 spark line,那才是"看每个 server"。

## 验证清单

| 检查 | 命令 | 结果 |
|---|---|---|
| typecheck | `npm run typecheck` | ✅ 0 错 |
| lint | `npm run lint` | ✅ 0 错 0 警 |
| test | `npm run test` | ✅ **37/37 passed** (新增 4 DashboardServersCard 测试)|
| openapi:check | `npm run openapi:check` | ✅ 3/3 |
| audit:catchup | `npm run audit:catchup` | ✅ 0 ERROR / 0 WARN / 3 INFO(误报)|

## 4 个 DashboardServersCard 测试

| 用例 | 验证 |
|---|---|
| mock data 7 个点 → 渲染 count + spark + delta | 端到端渲染流 |
| loading=true → 不渲染 spark(显示 el-skeleton) | 加载态分支 |
| count=0 + data=空 → 显示 0,spark 不渲染 delta | 空数据降级 |
| data 单点 → 不渲染 delta | < 2 个点不画线 |

## 风险与对策

| 风险 | 处理 |
|---|---|
| 后端 18080 未启动时 metrics 失败 | catch + sparkHistory.value = [] 空态,不影响 4 个 KPI |
| 拉取第一个 server 不具代表性 | meta label "CPU 7d" 不标"代表"避免误导 |
| DashboardView.vue 行数从 525 → 565 略涨 | 不动(Sprint 4 audit 误报修复一并处理) |
| `displayedTotal` 内部仍用 | 保留(animateCounter 内部需要),不再外传 |

## 后续

- Sprint 4:Polish 5(GitHub remote)+ Polish 6(audit 误报)
- 后端 Agent 真实接入:spark 才有真实历史数据来源
- Dashboard 4 个 KPI 卡片 polish(可选)

## 关联 commit

- `3dab5c8 feat(web): Sprint 3 Dashboard spark 接真实 + ServerSparkLine 复用 + 4 单测`
- `xxx docs(web): Sprint 3 dev-log + README 同步`
