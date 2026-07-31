# 2026-07-31 告警恢复后不再触发（alert_states 行未清除）

**日期**：2026-07-31
**严重度**：高（告警功能部分失效：所有规则在首次恢复后永久不再告警）
**发现途径**：MVP-9 性能基线执行前复核 `api-test/verify-alert-ws.mjs`（24 项）时，二次越界等待 `alert.push` 超时
**修复 commit**：`f7dba69`

## 现象

- 首次越界正常：产生 `alert_records` 记录 + `alert.push` 推送。
- 恢复（低于阈值）正常：记录变 `resolved`。
- **恢复后再次越界：无新记录、无 `alert.push`，5 秒超时**。

## 根因

`AlertStateMachine.evaluate` 的 Trigger 分支要求 `currentState == null`（首次越界语义）：

```java
if (breached && currentState == null) {
    return new AlertTransition.Trigger(rule, currentValue);   // 仅无状态行时触发
}
```

但 `handleResolve` 只执行 `updateStateResolved`（`active=0`）而**不删除状态行**：

```java
stateMapper.updateStateResolved(state.getId(), now, state.getVersion()); // 仅置 inactive
```

于是恢复后状态行仍存在（`active=0`），再次越界时：
- Trigger 分支：`currentState != null` → 不匹配
- ContinueBreached 分支：`active != true` → 不匹配
- Resolve 分支：`breached` → 不匹配
- → 落入 `NoAction`，规则**永久失效**

状态机的 javadoc 与单元测试注释明确设计了"恢复后 state 被清除、下次评估为 null"（`breachAfterRecoveryShouldReturnTrigger` 测试直接传 `null` 状态），但实现从未删除行——设计意图与实现脱节。

## 修复

恢复语义改为**乐观锁删除状态行**，与设计意图一致：

```sql
-- AlertStateMapper.xml
DELETE FROM alert_states WHERE id = #{id} AND version = #{version}
```

`handleResolve`：`updateStatusToResolved`（记录置 resolved）+ `deleteState`（删除状态行，恢复后下次评估为 null → 可再次 Trigger）。

## 验证

- 单元测试：`AlertEvaluationServiceTests` 恢复用例改为验证 `deleteState`；新增 H2 真实 SQL `deleteStateShouldRemoveRowWithVersionMatch`（版本不匹配不删、删除后查询为 null）。**341 tests 全绿**。
- 真实链路：`verify-alert-ws.mjs` 24/24 PASS（含"恢复后二次越界产生新记录"断言）。
- 性能基线场景 5（恢复→再触发 ×10）与场景 6（幂等 + 越界/恢复交替 ×50）正确性断言全过。

## 教训与预防

1. **"MockMvc 全绿 ≠ 真实行为正确"的又一例证**：状态机单测传 `null` 状态断言 Trigger，掩盖了"恢复后状态行从未被清除"的实现缺陷——测试模拟了设计意图而非真实数据流。
2. **复盘动作**：`AlertStateMachine` javadoc 中"RESOLVED → IDLE（state 被清除后下次评估为 null）"的语义必须与 `handleResolve` 实现强一致；建议后续在状态机测试中补充"active=false 状态行 + 越界"断言（当前机器对该输入返回 NoAction，属防御性兜底，不应成为依赖行为）。
3. 2026-07-28 交接文档曾声称"状态机恢复后不再触发"已修复，本次复核证明该修复并未落地（状态机自创建起未改动过）——**交接声明必须以复核验证为准**。

## 数据影响

- 验证库中本次运行产生的 `active=0` 残留状态行不影响新服务器/新规则（每次运行新建）。
- 生产尚未部署（域名备案中），无历史数据影响；若将来有存量 `active=0` 状态行，删除后即可恢复规则。
