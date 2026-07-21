// Package wsclient 管理 SuSuMonitor Agent 与后端的 WebSocket 连接。
//
// 连接地址为 ws://<backend>/ws/agent。首帧发送 agent.authenticate，
// 鉴权成功后可发送 heartbeat 和 metrics.report。
// 阶段 0 只定义消息结构；连接和重连在阶段 2 实现。
package wsclient

import "encoding/json"

// AgentMessage 是 WebSocket 通用消息结构，与后端协议文档一致。
//
// 字段说明见 docs-SuMon/Protocol-SuMon/websocket-protocol.md。
type AgentMessage struct {
	Type      string          `json:"type"`
	MessageID string          `json:"message_id"`
	Timestamp string          `json:"timestamp"`
	Payload   json.RawMessage `json:"payload"`
}

// AuthPayload 是 agent.authenticate 首帧的 payload。
type AuthPayload struct {
	ServerID int64  `json:"server_id"`
	Token    string `json:"token"`
}

// HeartbeatPayload 是 heartbeat 消息的 payload，为空对象。
type HeartbeatPayload struct{}

// MetricsPayload 是 metrics.report 消息的指标载荷，与后端固定宽表一一对应。
//
// 指针类型字段表示可空；Windows 上 temperature 和 load_avg 通常为 nil。
type MetricsPayload struct {
	ServerID      int64    `json:"server_id"`
	CollectedAt   string   `json:"collected_at"`
	CPUPercent    *float64 `json:"cpu_percent"`
	MemoryPercent *float64 `json:"memory_percent"`
	MemoryUsed    *uint64  `json:"memory_used"`
	MemoryTotal   *uint64  `json:"memory_total"`
	DiskPercent   *float64 `json:"disk_percent"`
	DiskUsed      *uint64  `json:"disk_used"`
	DiskTotal     *uint64  `json:"disk_total"`
	NetRx         *uint64  `json:"net_rx"`
	NetTx         *uint64  `json:"net_tx"`
	Temperature   *float64 `json:"temperature"`
	LoadAvg       *float64 `json:"load_avg"`
}
