# 开发日志: Agent WebSocket 真实验收

**日期**: 2026-07-21

**操作人**: opencode

## 改动内容

### 新增
- 在 `api-test` 新增独立 Node.js 验收工程，使用 `ws` 驱动真实 WebSocket 客户端。
- 新增 `verify-agent-ws.mjs`，自动完成管理员登录、创建隔离测试服务器、注册 Agent Token、Agent 鉴权、心跳、Metrics 上报和 Token 轮换失效验证。
- 新增 `package-lock.json` 锁定 `ws` 依赖版本。

## 涉及文件

- `api-test/package.json`
- `api-test/package-lock.json`
- `api-test/verify-agent-ws.mjs`
- `docs-SuMon/Develop-log/20260721-Agent-WebSocket真实验收.md`

## 验收范围

- 连接 `/ws/agent`。
- 使用首帧 `agent.authenticate` 完成 Agent Token 鉴权。
- 收到 `agent.authenticated`，响应时间为 UTC。
- 发送 `heartbeat` 并收到 `heartbeat.ack`。
- 发送固定宽表 `metrics.report`。
- 通过真实 REST 查询验证 latest/history 与上报数据一致。
- 数据库验证 Metrics 固定宽表落库。
- 轮换 Agent Token 后，旧 Token 重连被关闭码 1008 拒绝。

## 实际结果

```json
{"status":"PASS","server_id":6,"checks":14,"token_values_logged":false}
```

数据库抽查：

```text
server_id=6
cpu_percent=35.20
memory_percent=48.10
disk_percent=61.40
net_rx=123456
net_tx=654321
agent_token_rotated_at=已更新
```

验证层级：真实 Node.js WebSocket 客户端 + 真实 HTTP + 真实 MySQL 隔离库。

## 安全与留痕

- 管理员凭据和 Agent Token 只通过环境变量或内存变量传递。
- 验收脚本及输出不打印 JWT、Agent Token、密码或哈希全文。
- 项目工作树敏感值扫描无匹配。
- 隔离数据库 `susumonitor_agent_ws_validation_20260721` 保留，未执行删除。

## 当前进度

`/ws/agent` 真实连接验收完成。下一步验收 30 秒一次性 Monitor Ticket、订阅注册和数据库提交后的 `metrics.update` 广播。
