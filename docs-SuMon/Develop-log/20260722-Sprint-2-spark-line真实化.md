# 开发日志: Sprint 2 — ServerListView spark line 真实化

**日期**: 2026-07-22
**操作人**: opencode
**关联计划**: `docs-SuMon/Develop-plans/20260724-前端正式功能开发规划.md` Sprint 2

## 状态

| 项 | 状态 |
|---|---|
| `components/ServerSparkLine.vue` 通用组件 | ✅ 新增(168 行)|
| `components/ServerSparkLine.spec.ts` 单元测试 | ✅ 新增(50 行,5 测试)|
| `ServerListView.vue` CPU 趋势列 | ✅ 新增 61 行(spark + reload 拉取) |
| 4 件套 | ✅ typecheck 0 / lint 0 / **33/33 测试** / 3/3 openapi |
| audit:catchup | ✅ 0/0/3(3 个 LONG_FILE 误报,warn 级) |

**前端 spark line 从 mock 接真实后端 API**。

## 本次落地的工程文件

### 新增文件(2)

| 文件 | 行数 | 说明 |
|---|---|---|
| `src/components/ServerSparkLine.vue` | 168 | 通用 SVG spark 组件,props: `data: number[]` + `label` + `showMeta` |
| `src/components/ServerSparkLine.spec.ts` | 50 | 5 个单元测试(空数据/折线/趋势/label/showMeta)|

### 修改文件(1)

| 文件 | 改动 | 行数 |
|---|---|---|
| `src/views/ServerListView.vue` | 加 `cpuHistory(serverId)` + `loadAllSparkHistories()` + spark 列 | +61 |

**总:+279 行,3 文件**。

## 关键设计要点

### 1. ServerSparkLine 通用化

参考 `DashboardServersCard.vue` 的 spark 算法,抽出为独立组件,接受任意数值数组:

```typescript
defineProps<{
  data: number[]        // 必需,2+ 个点
  label?: string       // 可选,显示在 meta 区域
  showMeta?: boolean   // 默认 true,显示 label + delta
}>()
```

**3 个测试覆盖**:
- 空数据(< 2 个点)不渲染 polyline/path
- 2+ 数据点渲染 + 验证 points 数 = data 长度
- delta 计算:末尾 - 首位,正数 +up class、负数 +down class
- showMeta=false 隐藏 delta
- label 透传到 meta 文字

### 2. ServerListView 并发拉取历史(避免阻塞列表展示)

```typescript
async function loadAllSparkHistories(): Promise<void> {
  const ids = serverItems.value.map((s) => s.id)
  if (ids.length === 0) return
  const end = new Date()
  const start = new Date(end.getTime() - 7 * 24 * 60 * 60 * 1000)
  const results = await Promise.allSettled(
    ids.map((id) => getMetricsHistory(id, start.toISOString(), end.toISOString(), 1, 200))
  )
  // Map<serverId, cpu_series> + 失败不阻塞其他
}
```

**3 个关键设计**:
- `Promise.allSettled` 而非 `Promise.all`:单个 server 失败不影响其他
- 失败不写 Map:组件拿不到数据就显示空态,不显示错误
- `void loadAllSparkHistories()` 在 reload 末尾 fire-and-forget:列表立即展示,spark 异步填充

### 3. 性能考虑

- 拉取范围 7 天 × page_size=200 = 最多 200 个点(实际取 cpu_percent 序列)
- `getMetricsHistory` 是已有 API,无需新增
- 单次 `Promise.allSettled` 并发:N 个 server 同时拉
- 失败容错:单个 server 失败不影响其他

## 验证清单

| 检查 | 命令 | 结果 |
|---|---|---|
| typecheck | `npm run typecheck` | ✅ 0 错 |
| lint | `npm run lint` | ✅ 0 错 0 警 |
| test | `npm run test` | ✅ **33/33 passed** (新增 5 spark 测试)|
| openapi:check | `npm run openapi:check` | ✅ 3/3 |
| audit:catchup | `npm run audit:catchup` | ✅ 0 ERROR / 0 WARN / 3 INFO(误报)|

## 风险与对策

| 风险 | 处理 |
|---|---|
| 后端 18080 未启动时,所有 history 请求失败 | Promise.allSettled 兜底,失败不抛错,spark 显示空态 |
| 0 数据点的 server | ServerSparkLine 自动隐藏 polyline(测试已覆盖) |
| CPU 为 null(传感器缺失) | `.filter((v): v is number => v !== null)` 过滤 null |
| reload 频繁触发(搜索/排序) | loadAllSparkHistories 每次重跑,Map 覆盖式更新 |
| N 个 server 拉 N 次 API | N≤10(开发环境),并发,总时间 < 1s(估算) |

## 后续

- Sprint 3:DashboardView 完整化(下周) — spark 接真实数据 + 4 个 KPI 卡 polish
- Sprint 4:Polish 5(GitHub)+ Polish 6(audit 误报)
- 后端 Agent 真实接入:有数据后 spark line 才有真实意义

## 关联 commit

- `c99fa03 feat(web): Sprint 2 ServerListView spark line 接真实 /api/servers/{id}/metrics 历史 + 5 单测`
- `xxx docs(web): Sprint 2 dev-log + README 同步`
