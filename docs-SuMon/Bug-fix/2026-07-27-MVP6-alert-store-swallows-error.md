# MVP6-alert-store-swallows-error `AlertRuleDialog` 40900 中文 toast 永远不可达（已修复）

**日期**: 2026-07-27
**发现方式**: opencode 在 MVP-6 收口盘点中发现的双层 try/catch 不可达分支
**优先级**: 高（影响 40900 / 40001 / 40400 / 403 全部错误码的精确中文 toast 文案）
**模块**: 告警前端 store + 告警前端 dialog
**影响前端**: `AlertRuleDialog.vue` 创建/编辑两条主路径，所有错误反馈退化为通用文案

## 修复记录（2026-07-27）

- `web-vue-SuMon/src/stores/alerts.ts` 的 `createRule` / `updateRule` / `deleteRule` 三个 action 由"吞错返回 null/boolean"改为"设置 `rulesError` 后**重抛**原错误"。
- 返回类型严格化：`createRule` / `updateRule` 由 `Promise<AlertRule | null>` 收紧为 `Promise<AlertRule>`；`deleteRule` 保留 `Promise<boolean>`（调用方语义不变），但失败时同样抛出。
- `AlertRuleDialog.vue:285-318` 的 `handleSubmit` 维持现状，靠外层 `try/catch` 的 `explainError(error)` 捕获 store 重抛，输出精确中文 toast。原先的 `if (ok === null)` / `if (created === null)` inner 分支在新类型下成为编译期不可达代码，清理掉。
- 单测 `web-vue-SuMon/src/stores/alerts.spec.ts` 已更新类型断言（`?.` 去掉），并新增 createRule/updateRule/deleteRule 失败重抛回归用例。

## 复现命令

```powershell
# 用户在浏览器触发任意"创建规则"操作，故意制造 40900 资源冲突场景
# 例如:同 serverId 同 metric 同 operator 已存在一条规则，再创建一条
```

## 期望行为

| 后端响应 | 期望 toast 文案 |
|---|---|
| `40900 RESOURCE_CONFLICT` | `规则冲突:同一指标 / 服务器的同类规则已存在` |
| `40001 INVALID_REQUEST_PARAMETER` | `参数不合法,请检查表单字段` |
| `40400 RESOURCE_NOT_FOUND` | `服务器不存在或已被删除` |
| `40300 FORBIDDEN` | `无权限:仅管理员可管理告警规则` |
| 其他 ApiBusinessError | 后端 `message` 或通用 `操作失败` |
| 网络 / 其他异常 | `网络异常,请稍后重试` |

## 实际行为（修复前）

无论何种错误,都显示:

```text
规则创建失败
或
规则更新失败
或
规则删除失败
```

`AlertRuleDialog.vue:258-275` 已经实现了 `explainError(error)` 映射 `ApiBusinessError.code` 到中文文案,**但 store 内部吞掉了 `ApiBusinessError`** ,把 `null` / `false` 返回给 dialog,导致:

- `if (ok === null)` / `if (created === null)` 分支显示 `alerts.rulesError ?? '规则X失败'` 通用文案
- 外层 `catch (error)` 永远捕获不到 store 抛出的错误,`explainError` 路径不可达

## 静态分析锁定位置

### Bug 1：`stores/alerts.ts` 三个 action 吞错

**修改前**(3 个 action 都是同款 pattern):

```typescript
async function createRule(req: CreateAlertRuleRequest): Promise<AlertRule | null> {
  try {
    const response = await createAlertRule(req)
    rules.value = [response.data, ...rules.value]
    return response.data
  } catch (reason) {
    rulesError.value = reason instanceof Error ? reason.message : '规则创建失败'
    return null   // ← 吞掉 error
  }
}
```

`updateRule` 同样模式（line 93-107），`deleteRule` 返回 `false` 但同样吞掉 error（line 114-122）。

### Bug 2：`AlertRuleDialog.vue:285-318` 双层 try/catch 设计依赖 store 重抛

```typescript
try {
  if (isEdit.value && props.rule !== null) {
    const ok = await alerts.updateRule(...)
    if (ok === null) {                            // ← 假设 store 抛错
      ElMessage.error(alerts.rulesError ?? '规则更新失败')
      return
    }
    ElMessage.success('规则已更新')
  } else {
    const created = await alerts.createRule(...)
    if (created === null) {                       // ← 假设 store 抛错
      ElMessage.error(alerts.rulesError ?? '规则创建失败')
      return
    }
    ElMessage.success('规则已创建')
  }
  emit('success')
  emit('update:modelValue', false)
} catch (error) {                                 // ← 真正的精确错误映射
  ElMessage.error(explainError(error))
}
```

设计意图是把"通用兜底"放 inner，"精确中文映射"放 outer。**但 Bug 1 让 outer 永远收不到**。

## 探测证据

`docs-SuMon/Bug-fix/2026-07-27-MVP6-alert-rules-mapper-500.md` 修复后,真实 MySQL HTTP 写路径仍存在"无法看到精确错误"的问题——例如 40900 资源冲突,前端只会显示"规则创建失败"而非"规则冲突:同一指标 / 服务器的同类规则已存在"。

具体复现:同 server_id / metric / operator 创建两条规则,第二条会被后端业务校验拒绝并返回 40900,前端 toast 应当是中文精确文案,实际是通用"规则创建失败"。

## 影响范围

### 前端

- `AlertRuleDialog.vue` 创建 / 编辑 失败时,4 个错误码(40900 / 40001 / 40400 / 403)全部退化为通用文案
- 用户无法区分"我真的配错了"和"系统拒绝",调试体验极差

### 后端

- 不涉及,后端业务错误码映射正确

## 单元测试为何未发现

| 测试层 | 验证了什么 | 漏掉了什么 |
|---|---|---|
| `AlertRuleDialog` | 无视图级单测 | 双层 try/catch 设计未验证 |
| `stores/alerts` 单测 | `expect(ok).toBe(false)` / `expect(created?.id).toBe(99)` | 未验证 store 是否把 error 重抛给调用方 |

`AlertRuleDialog.vue` 当前**没有**任何组件级单测(view 维度),这是测试盲区——但即使有,断言 inner 分支会通过(因为 store 确实返回了 null),所以**只有修改 store 让 dialog catch 路径可达,才能在 P2 真实联调阶段 14 项(40900 toast)实际验证**。

## 修复方向（已实施）

1. **已实施**：`stores/alerts.ts` `createRule` / `updateRule` / `deleteRule` 三个 action 改为 catch 中设置 `rulesError` + 重抛原 error,返回类型严格化为 `Promise<AlertRule>`。
2. **已实施**：`AlertRuleDialog.vue` 清理 inner `if (... === null)` 死代码,完全依赖 outer catch + `explainError(error)`。
3. **已实施**：`stores/alerts.spec.ts` 更新现有 `expect(created?.id).toBe(99)` 适配新类型,新增 createRule / updateRule / deleteRule 三个失败重抛回归用例。
4. **P2 验证**：用户在浏览器触发 40900 / 40001 / 40400 / 403 实际场景,确认 toast 显示精确中文文案——这一项是 plan 阶段 1-4 第 14 项"错误码映射"的通过标准。

## 前端绕过方案

修复前:无。dialog 错误展示就是这样设计,前端无法绕过。

## 修复后前端收益

- 40900 RESOURCE_CONFLICT → "规则冲突:同一指标 / 服务器的同类规则已存在"
- 40001 INVALID_REQUEST_PARAMETER → "参数不合法,请检查表单字段"
- 40400 RESOURCE_NOT_FOUND → "服务器不存在或已被删除"
- 40300 FORBIDDEN → "无权限:仅管理员可管理告警规则"
- 其他 ApiBusinessError → 后端 message 或 "操作失败"
- 网络异常 → "网络异常,请稍后重试"

## 验收标准

修复完成后:

- [x] `createRule` / `updateRule` 失败时 store 重抛 `ApiBusinessError`
- [x] `deleteRule` 失败时 store 重抛 `ApiBusinessError`
- [x] `AlertRuleDialog` 移除 inner null 检查,outer catch 接管错误展示
- [x] `stores/alerts.spec.ts` 新增 3 个失败重抛回归用例,10 + 3 = 13 → ≥13 个 store 用例
- [x] 前端 5 件套全绿
- [ ] P2 真实联调:用户在浏览器触发 40900 / 40001 / 40400 / 403 实际场景,确认 toast 显示精确中文文案

## 关联

- 问题单(后端):`docs-SuMon/Bug-fix/2026-07-27-MVP6-alert-rules-mapper-500.md`
- 修复日志(后端):`docs-SuMon/Develop-log/20260727-MVP6告警规则Mapper修复.md`
- 提交 checklist:`docs-SuMon/Bug-fix/2026-07-27-MVP6-alert-rules-mapper-500-commit-checklist.md`
- 收口计划:`C:\Users\genhaosan\.claude\plans\iterative-bouncing-valley.md`
