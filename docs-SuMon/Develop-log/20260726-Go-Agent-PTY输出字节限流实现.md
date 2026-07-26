# Go Agent PTY 输出字节限流实现

**日期**: 2026-07-26
**状态**: 已实现并完成 Windows 与 WSL Linux 验证

## 目标

为每个 Linux PTY 会话独立限制上行原始输出字节，避免单个高输出终端会话占用 Agent 到服务端的通信带宽。

## 实现

- 新增 `SUSUMONITOR_TERMINAL_OUTPUT_RATE_BYTES_PER_SECOND`，默认 `262144`（256 KiB/s）。
- 新增 `SUSUMONITOR_TERMINAL_OUTPUT_BURST_BYTES`，默认 `524288`（512 KiB）。
- 配置要求限速为正数，且突发字节数不少于单块最大 PTY 输出字节数。
- 每个 PTY 会话创建时拥有独立、初始满额的 Token Bucket。
- `forwardOutput` 在调用上层 Callback 前按原始 `[]byte` 长度消耗令牌；令牌不足不等待、不输出该块，立即以 `output_rate_exceeded` 关闭会话。
- 令牌桶内部支持注入时钟；测试可确定性验证按秒补充，运行时仍使用系统时钟。
- 未增加终端输入、输出、命令或 Shell 环境的日志记录。

## 备份

- `C:\Backup\SuSuMonitor\20260726-agent-output-rate-limit-review.zip`

## 涉及文件

- `agent-go-SuMon/internal/config/config.go`
- `agent-go-SuMon/internal/config/config_test.go`
- `agent-go-SuMon/internal/terminal/terminal.go`
- `agent-go-SuMon/internal/terminal/manager_linux.go`
- `agent-go-SuMon/internal/terminal/manager_linux_test.go`
- `agent-go-SuMon/cmd/susumonitor-agent/terminal_agent.go`
- `agent-go-SuMon/.env.example`
- `agent-go-SuMon/deploy/agent.env`
- `docs-SuMon/Protocol-SuMon/websocket-protocol.md`

## 验证

- 已通过（Windows）：在 `agent-go-SuMon` 执行 `go test ./...`，所有包通过；Windows 覆盖非 Linux 终端拒绝路径、配置校验及通用 Agent 逻辑。
- 已通过（WSL Ubuntu）：`/opt/go/bin/go version` 为 `go1.23.0 linux/amd64`；执行 `/opt/go/bin/go test ./...` 通过，覆盖真实 Linux PTY 与输出限流测试。
- 已通过（WSL Ubuntu）：执行 `/opt/go/bin/go test -race ./...` 通过，未报告数据竞争。
- 定向覆盖：初始突发放行、超额输出不回调、关闭原因 `output_rate_exceeded`、令牌按时间补充、会话间桶隔离、无效速率和突发配置拒绝。
- 未覆盖：Java 输出限速、Monitor 慢消费者背压和真实 Java-Go 高输出压测，属于后续独立模块及最终专项验收。
