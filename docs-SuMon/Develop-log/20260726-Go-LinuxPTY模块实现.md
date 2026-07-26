# Go Linux PTY 模块实现

**日期**: 2026-07-26
**状态**: 已实现并完成跨平台验证

## 目标

让 Linux Go Agent 在受保护本地配置的固定 Shell 上创建 root PTY，并通过既有 `/ws/agent` 终端协议处理打开、输入、输出、resize、关闭和进程回收。

## 实现

- 引入固定版本 `github.com/creack/pty v1.1.24`。
- Linux `terminal.Manager` 维护会话上限、PTY、进程组、输出队列、空闲超时和最大生命周期。
- 非 Linux 构建使用 `unsupportedManager` 明确拒绝终端操作，不尝试 Windows ConPTY。
- PTY 输出使用有界队列；队列溢出时关闭该会话而非无限累积内存。
- 关闭时向整个 PTY 进程组发送 `SIGTERM`，五秒未退出时发送 `SIGKILL`。
- Agent 仅接受 Java 下发的匹配 `server_id` 的控制消息。
- Agent 保持本地绝对 Shell 路径 `/bin/bash`，但 `terminal.opened.shell` 仅回传 `bash` 标识，避免协议路径泄露并满足 Java 字段约束。
- Linux systemd 服务使用 `root`，`/etc/susumonitor/agent.env` 使用 `root:root` 与 `0600`。

## 备份

- `C:\Backup\SuSuMonitor\20260726-agent-pty-module.zip`
- `C:\Backup\SuSuMonitor\20260726-agent-terminal-shell-identifier.go`
- `C:\Backup\SuSuMonitor\20260726-agent-pty-plan-before-update.md`

## 验证边界

- Windows 常规 Go 测试仅验证非 Linux 明确拒绝终端操作。
- WSL Linux 测试验证真实 `/bin/bash` PTY 的输入、输出、resize、关闭和会话上限。
- Java-Go WSL 真实联调记录见 `20260726-终端Java-Go真实联调准备与阻塞记录.md`。
- 输出带宽限制、Java 输出限速与 Monitor 慢消费者保护属于后续独立模块。

## 验证记录

- Windows：`go test ./...` 通过。
- WSL Linux：`go test ./...` 通过。
- WSL Linux：`go test -race ./...` 通过。
- 真实 Java-Go WSL PTY：已验证 `open -> input/output -> resize -> close`，结果为 `PTY_RELAY_INTEGRATION_OK`。
