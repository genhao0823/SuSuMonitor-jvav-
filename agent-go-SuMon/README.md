# SuSuMonitor Agent (Go)

SuSuMonitor 监控采集 Agent，Go 跨平台实现。

## 技术栈

- Go 1.22+
- gopsutil（跨平台系统指标采集，阶段 3 引入）
- coder/websocket（WebSocket 客户端，阶段 2 引入）

## 当前阶段

阶段 0：工程骨架。仅用标准库，能编译通过。WebSocket 连接和指标采集在后续阶段实现。

## 目录结构

```text
agent-go-SuMon/
├── go.mod
├── README.md
├── .env.example
├── Makefile
├── config/
│   └── agent.example.yml
├── cmd/
│   └── susumonitor-agent/
│       └── main.go
├── internal/
│   ├── config/       配置加载与校验
│   ├── wsclient/     WebSocket 连接、鉴权、重连
│   ├── collector/    系统指标采集
│   └── reporter/     metrics.report 消息构造与上报
└── bin/              构建产物（不提交 Git）
```

## 构建

```powershell
# 编译
go build -o bin/susumonitor-agent.exe cmd/susumonitor-agent/main.go

# 或用 Makefile
make build
```

## 配置

环境变量优先于配置文件。

```powershell
# 从 .env.example 复制并填入真实值
cp .env.example .env
# 或用配置文件
cp config/agent.example.yml config/agent.yml
```

关键配置项：

| 配置项 | 说明 |
|--------|------|
| `SUSUMONITOR_BACKEND_URL` | 后端 WebSocket 地址，如 `ws://localhost:18080` |
| `SUSUMONITOR_SERVER_ID` | 服务器 ID（admin 预建后获得） |
| `SUSUMONITOR_AGENT_TOKEN` | Agent Token（admin 通过 REST 预发放，明文仅一次性返回） |

## 协议

Agent 通过 `ws://<backend>/ws/agent` 连接后端，首帧发送 `agent.authenticate`，鉴权成功后每 5 秒上报 `metrics.report`，每 30 秒发送 `heartbeat`。

详见 `docs-SuMon/Protocol-SuMon/websocket-protocol.md`。

## 安全

- `agent_token` 不写日志，只输出前 8 字符用于排障。
- 配置文件（`.env`、`config/agent.yml`）不入 Git。
- 日志不输出密钥、密码、私钥。
