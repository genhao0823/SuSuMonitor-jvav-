# Agent WebSocket 客户端实现

**日期**：2026-07-21
**操作人**：OpenCode
**活动类型**：功能实现

## 一、活动范围

按 `docs-SuMon/Develop-plans/20260721-Agent-Go监控采集器开发计划.md` 阶段 2，实现 Go Agent 的 WebSocket 客户端，包含连接、首帧鉴权、心跳和指数退避重连。

## 二、技术栈

| 依赖 | 版本 | 用途 |
|------|------|------|
| `github.com/coder/websocket` | v1.8.15 | WebSocket 连接、JSON 收发（`websocket.Dial`、`wsjson.Write`、`wsjson.Read`） |
| 标准库 `crypto/rand` | — | UUID v4 生成，不引入第三方 UUID 库 |
| 标准库 `encoding/json` | — | JSON 序列化 |
| 标准库 `log/slog` | — | 结构化日志 |
| 标准库 `time`/`context` | — | 定时器、超时控制 |

go.mod 因 coder/websocket v1.8.15 的最低版本要求，从 `go 1.22` 升级到 `go 1.23`（本机 go1.26.4 满足）。

## 三、改动内容

### 3.1 提交 1：引入依赖 + 消息构造 + 单元测试

commit `7e8ca8a`

| 文件 | 改动 |
|------|------|
| `agent-go-SuMon/go.mod` | 新增 `github.com/coder/websocket v1.8.15`，go 版本升级到 1.23 |
| `agent-go-SuMon/go.sum` | 新建，依赖哈希 |
| `agent-go-SuMon/internal/wsclient/message.go` | 追加 `newMessage`/`newAuthMessage`/`newHeartbeatMessage`/`newUUID` 函数 |
| `agent-go-SuMon/internal/wsclient/message_test.go` | 新建，5 个测试用例 |

### 3.2 提交 2：实现 Client 完整逻辑

commit `d3e2e47`

| 文件 | 改动 |
|------|------|
| `agent-go-SuMon/internal/wsclient/client.go` | 重写，实现 Run/connectAndAuthenticate/runLoops/sendHeartbeat/handleMessage |

### 3.3 提交 3：main.go 启动 Client + 文档同步

| 文件 | 改动 |
|------|------|
| `agent-go-SuMon/cmd/susumonitor-agent/main.go` | 从"等待信号"改为启动 `wsclient.NewClient` + `client.Run(ctx)` |
| `docs-SuMon/Develop-plans/20260721-Agent-Go监控采集器开发计划.md` | 阶段 0/1/2 标记完成状态 |
| `docs-SuMon/Develop-log/20260721-Agent-WebSocket客户端实现.md` | 新建本开发日志 |

## 四、关键设计

### 4.1 消息构造

- `newMessage(msgType, payload)`：`json.Marshal(payload)` → 填入 `AgentMessage{Type, MessageID: newUUID(), Timestamp: time.Now().UTC().Format(time.RFC3339Nano), Payload}`
- `newUUID()`：`crypto/rand.Read(16字节)` → 设置 v4 版本位和变体位 → `fmt.Sprintf`，不引入 `google/uuid`
- 时间格式 `RFC3339Nano`（如 `2026-07-21T12:00:00.123456789Z`）与后端 `OffsetDateTime.toString()` 兼容

### 4.2 Client 状态机

```text
Disconnected → Connecting → Authenticating → Connected → Reconnecting
```

- `Run` 循环：`connectAndAuthenticate` → 成功重置退避 → `runLoops` → 断线退避重连
- `connectAndAuthenticate`：`websocket.Dial` → `wsjson.Write(authMsg)` → `wsjson.Read(&resp)` → 检查 `agent.authenticated`
- `runLoops`：2 个 goroutine（心跳 ticker + 接收循环），任一出错通过 `errCh` 返回
- 退避：`reconnectInitial` → `*2` → 上限 `reconnectMax`，Go 1.22+ 内置 `min` 函数

### 4.3 常量与后端对齐

| 常量 | 值 | 后端对应 |
|------|-----|----------|
| `wsPath` | `/ws/agent` | `AgentWebSocketConfig.java:25` |
| `authTimeout` | 10 秒 | `AgentWebSocketHandler.java:122` 首帧超时 |
| `readTimeout` | 95 秒 | 略大于后端 90 秒离线判定 |
| `writeTimeout` | 5 秒 | 防止发送阻塞 |

### 4.4 日志脱敏

- `agent_token` 不写日志，main.go 只输出 `cfg.TokenPrefix()`（前 8 字符）
- 心跳日志只输出 `message_id`，不输出 token
- 错误消息从 payload 提取 `message` 字段，不输出完整 payload

## 五、备份

修改前已备份到 `C:\Backup\SuSuMonitor\20260721-180043\`：

- `go.mod`（31 字节）
- `internal/wsclient/message.go`（1752 字节）
- `internal/wsclient/client.go`（1508 字节）
- `cmd/susumonitor-agent/main.go`（1491 字节）

所有备份文件已验证存在、非空、可读取。

## 六、验证

| 验证对象 | 命令 | 结果 |
|----------|------|------|
| 单元测试 | `go test ./internal/wsclient/ -v` | 5/5 通过（TestNewAuthMessage、TestNewHeartbeatMessage、TestNewUUID、TestMetricsPayloadNullFields、TestAgentMessageJSONFieldNames） |
| 编译 | `go build ./...` | exit 0 |
| 全量测试 | `go test ./...` | wsclient 通过，其他包无测试文件 |
| 静态检查 | `go vet ./...` | exit 0 |
| husky pre-commit | OpenAPI 契约校验 | 3/3 通过 |

## 七、未完成或后续

- `config_test.go` 单元测试待补（阶段 1 标记"已实现，单元测试待补"）。
- `SendMetrics` 方法保持 `not implemented`（阶段 4 接入 reporter 后实现，届时需将 conn 存入 Client 并加写锁）。
- 真实后端运行时连接验证未执行（需启动后端 + 预发放 Agent Token）。
- 阶段 3（指标采集）和阶段 4（指标上报）待执行。
- 阶段 5（链路冒烟测试）待执行。
