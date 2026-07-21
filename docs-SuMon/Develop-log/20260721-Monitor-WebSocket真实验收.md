# 开发日志: Monitor WebSocket 真实验收

**日期**: 2026-07-21

**操作人**: opencode

## 改动内容

### 新增
- 新增 `verify-monitor-ws.mjs`，使用 Node.js `ws` 驱动 Monitor 和 Agent 两条真实 WebSocket 连接。
- 脚本自动创建普通用户、管理员审批、用户登录、申请 Monitor Ticket、订阅服务器、上报 Metrics、取消订阅和重复使用 Ticket。
- 在 `api-test/package.json` 增加 `verify:monitor-ws` 命令。

## 涉及文件

- `api-test/package.json`
- `api-test/verify-monitor-ws.mjs`
- `docs-SuMon/Develop-log/20260721-Monitor-WebSocket真实验收.md`

## 验收范围

- 普通用户注册后状态为 pending。
- 管理员审批用户，approved 用户可登录。
- `POST /api/ws/monitor-ticket` 返回 30 秒一次性 Ticket，`expires_at` 为 UTC。
- 使用 Ticket 连接 `/ws/monitor`。
- 发送 `metrics.subscribe` 建立 server_id 订阅。
- Agent 通过 `/ws/agent` 鉴权并上报 Metrics。
- Monitor 收到 `metrics.update`，server_id、CPU 和 collected_at 与上报一致。
- `metrics.update.timestamp` 和 `collected_at` 均为 UTC。
- 发送 `metrics.unsubscribe` 后，第二条 Metrics 虽成功落库但 Monitor 不再收到更新。
- 重复使用同一 Ticket 握手返回 HTTP 401。

## 实际结果

```json
{"status":"PASS","server_id":7,"checks":16,"token_values_logged":false}
```

数据库抽查：

```text
server_id=7
metric_count=2
min_cpu=41.70
max_cpu=42.80
monitor_user_role=user
monitor_user_review_status=approved
```

服务日志抽查：一次性 Ticket 二次使用产生 1 条 `monitor ticket rejected` 留痕，日志未包含 Ticket 值。

## 验证边界

- 验证层级：真实 Node.js WebSocket 客户端 + 真实 HTTP + 真实 MySQL 隔离库。
- `metrics.update` 仅能通过“数据库存在数据后收到广播”间接验证 AFTER_COMMIT 顺序；事务回滚不广播仍需专门集成测试覆盖。
- 第一版仅支持单 JVM，未验证多实例状态共享。

## 安全与留痕

- JWT、Agent Token、Monitor Ticket 和密码只通过环境变量或内存变量传递。
- 脚本及输出不打印任何敏感值。
- 工作树敏感值扫描无匹配。
- 隔离数据库 `susumonitor_agent_ws_validation_20260721` 继续保留，未执行删除。

## 当前进度

Monitor Ticket、订阅注册、Metrics 实时广播和取消订阅的真实链路验收完成。下一步同步 OpenAPI、WebSocket 协议、HTTP 样例和总开发记录，并执行最终回归。
