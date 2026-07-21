// Package wsclient 的 client.go 定义 WebSocket 客户端结构。
//
// 阶段 0 只定义结构和方法签名，不做实际连接。
// 阶段 2 将引入 github.com/coder/websocket 并实现连接、鉴权、心跳和重连。
package wsclient

import (
	"context"
	"fmt"
	"log/slog"
)

// Client 管理 WebSocket 连接、鉴权、心跳和重连。
type Client struct {
	backendURL string
	serverID   int64
	token      string
	logger     *slog.Logger
	// 阶段 2 补充：conn *websocket.Conn
}

// NewClient 创建 WebSocket 客户端。
//
// backendURL 形如 ws://localhost:18080。
// token 是 admin 预发放的 Agent Token，不写入日志。
func NewClient(backendURL string, serverID int64, token string, logger *slog.Logger) *Client {
	return &Client{
		backendURL: backendURL,
		serverID:   serverID,
		token:      token,
		logger:     logger,
	}
}

// Run 启动连接、首帧鉴权和消息循环，阻塞直到 context 取消或不可恢复错误。
//
// 阶段 0 未实现；阶段 2 将实现完整状态机：
// Disconnected → Connecting → Authenticating → Connected → Reconnecting
func (c *Client) Run(ctx context.Context) error {
	return fmt.Errorf("not implemented: WebSocket connection (阶段 2)")
}

// SendMetrics 通过已认证连接发送 metrics.report 消息。
//
// 阶段 0 未实现；阶段 4 接入 reporter 后实现。
func (c *Client) SendMetrics(payload MetricsPayload) error {
	return fmt.Errorf("not implemented: metrics report (阶段 4)")
}
