// Package wsclient 的 client.go 实现 WebSocket 客户端。
//
// 客户端连接 ws://<backend>/ws/agent，首帧发送 agent.authenticate，
// 鉴权成功后每 heartbeatInterval 发送 heartbeat，并持续接收服务端消息。
// 连接断开后按指数退避重连。
package wsclient

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"time"

	"github.com/coder/websocket"
	"github.com/coder/websocket/wsjson"
)

const (
	// wsPath 是 Agent WebSocket 端点路径，与后端 AgentWebSocketConfig 一致。
	wsPath = "/ws/agent"
	// authTimeout 是首帧认证超时，与后端 AgentWebSocketHandler 的 10 秒清理一致。
	authTimeout = 10 * time.Second
	// writeTimeout 是单次 WebSocket 写操作超时。
	writeTimeout = 5 * time.Second
	// readTimeout 是接收循环的读超时，略大于后端 90 秒离线判定。
	readTimeout = 95 * time.Second
)

// Client 管理 WebSocket 连接、鉴权、心跳和重连。
type Client struct {
	backendURL        string
	serverID          int64
	token             string
	logger            *slog.Logger
	heartbeatInterval time.Duration
	reconnectInitial  time.Duration
	reconnectMax      time.Duration
}

// NewClient 创建 WebSocket 客户端。
//
// backendURL 形如 ws://localhost:18080，不含路径。
// token 是 admin 预发放的 Agent Token，不写入日志。
// heartbeatInterval 是心跳间隔，与后端 90 秒离线判定配合（建议 30 秒）。
// reconnectInitial 是重连初始间隔，reconnectMax 是重连最大间隔。
func NewClient(backendURL string, serverID int64, token string, logger *slog.Logger,
	heartbeatInterval, reconnectInitial, reconnectMax time.Duration) *Client {
	return &Client{
		backendURL:        backendURL,
		serverID:          serverID,
		token:             token,
		logger:            logger,
		heartbeatInterval: heartbeatInterval,
		reconnectInitial:  reconnectInitial,
		reconnectMax:      reconnectMax,
	}
}

// Run 启动连接、首帧鉴权和消息循环，阻塞直到 context 取消。
//
// 状态机：Disconnected → Connecting → Authenticating → Connected → Reconnecting。
// 连接或鉴权失败后按指数退避重连（reconnectInitial → *2 → 上限 reconnectMax）。
// 鉴权成功后重置退避为初始值。
func (c *Client) Run(ctx context.Context) error {
	backoff := c.reconnectInitial
	for {
		if err := ctx.Err(); err != nil {
			return err
		}

		conn, err := c.connectAndAuthenticate(ctx)
		if err != nil {
			c.logger.Warn("connect or authenticate failed, reconnecting",
				"error", err, "backoff", backoff)
			select {
			case <-time.After(backoff):
				backoff = min(backoff*2, c.reconnectMax)
			case <-ctx.Done():
				return ctx.Err()
			}
			continue
		}

		// 鉴权成功，重置退避。
		backoff = c.reconnectInitial
		c.logger.Info("agent authenticated", "server_id", c.serverID)

		err = c.runLoops(ctx, conn)
		conn.CloseNow()

		if err := ctx.Err(); err != nil {
			return err
		}
		c.logger.Warn("connection lost, reconnecting", "error", err)
	}
}

// connectAndAuthenticate 建立 WebSocket 连接并完成首帧鉴权。
//
// 流程：Dial → 发 agent.authenticate → 读 agent.authenticated → 返回 conn。
// 任何步骤失败都关闭连接并返回错误，由 Run 决定重连。
func (c *Client) connectAndAuthenticate(ctx context.Context) (*websocket.Conn, error) {
	url := c.backendURL + wsPath

	// Dial 超时与首帧认证超时一致。
	dialCtx, cancel := context.WithTimeout(ctx, authTimeout)
	defer cancel()

	conn, _, err := websocket.Dial(dialCtx, url, nil)
	if err != nil {
		return nil, fmt.Errorf("dial %s: %w", url, err)
	}

	// 发送首帧 agent.authenticate。
	authMsg := newAuthMessage(c.serverID, c.token)
	writeCtx, cancelWrite := context.WithTimeout(ctx, writeTimeout)
	defer cancelWrite()

	if err := wsjson.Write(writeCtx, conn, authMsg); err != nil {
		conn.CloseNow()
		return nil, fmt.Errorf("write authenticate: %w", err)
	}

	// 等待 agent.authenticated 响应。
	readCtx, cancelRead := context.WithTimeout(ctx, authTimeout)
	defer cancelRead()

	var resp AgentMessage
	if err := wsjson.Read(readCtx, conn, &resp); err != nil {
		conn.CloseNow()
		return nil, fmt.Errorf("read authenticated: %w", err)
	}

	if resp.Type != "agent.authenticated" {
		conn.CloseNow()
		return nil, fmt.Errorf("unexpected auth response type: %s", resp.Type)
	}

	return conn, nil
}

// runLoops 运行心跳和接收循环，任一出错即返回。
//
// 心跳 goroutine 每 heartbeatInterval 发送 heartbeat。
// 接收 goroutine 持续读取服务端消息（heartbeat.ack、error 等）。
func (c *Client) runLoops(ctx context.Context, conn *websocket.Conn) error {
	heartbeatTicker := time.NewTicker(c.heartbeatInterval)
	defer heartbeatTicker.Stop()

	errCh := make(chan error, 2)

	// 心跳 goroutine。
	go func() {
		for {
			select {
			case <-ctx.Done():
				errCh <- ctx.Err()
				return
			case <-heartbeatTicker.C:
				if err := c.sendHeartbeat(ctx, conn); err != nil {
					errCh <- fmt.Errorf("heartbeat: %w", err)
					return
				}
			}
		}
	}()

	// 接收 goroutine。
	go func() {
		for {
			if err := ctx.Err(); err != nil {
				errCh <- ctx.Err()
				return
			}
			var msg AgentMessage
			readCtx, cancel := context.WithTimeout(ctx, readTimeout)
			err := wsjson.Read(readCtx, conn, &msg)
			cancel()
			if err != nil {
				errCh <- fmt.Errorf("read: %w", err)
				return
			}
			c.handleMessage(msg)
		}
	}()

	return <-errCh
}

// sendHeartbeat 发送 heartbeat 消息。
func (c *Client) sendHeartbeat(ctx context.Context, conn *websocket.Conn) error {
	msg := newHeartbeatMessage()
	writeCtx, cancel := context.WithTimeout(ctx, writeTimeout)
	defer cancel()
	if err := wsjson.Write(writeCtx, conn, msg); err != nil {
		return err
	}
	c.logger.Debug("heartbeat sent", "message_id", msg.MessageID)
	return nil
}

// handleMessage 处理接收到的服务端消息。
//
// heartbeat.ack → Debug 日志；error → 提取 message 字段 → Warn 日志；default → Debug。
func (c *Client) handleMessage(msg AgentMessage) {
	switch msg.Type {
	case "heartbeat.ack":
		c.logger.Debug("heartbeat ack received", "message_id", msg.MessageID)
	case "error":
		var errPayload struct {
			Message string `json:"message"`
		}
		if err := json.Unmarshal(msg.Payload, &errPayload); err == nil && errPayload.Message != "" {
			c.logger.Warn("server error", "message", errPayload.Message)
		} else {
			c.logger.Warn("server error", "raw_payload", string(msg.Payload))
		}
	default:
		c.logger.Debug("received message", "type", msg.Type)
	}
}

// SendMetrics 通过已认证连接发送 metrics.report 消息。
//
// 阶段 2 未实现；阶段 4 接入 collector 和 reporter 后实现，
// 届时需要将 conn 存入 Client 并加写锁保证并发安全。
func (c *Client) SendMetrics(payload MetricsPayload) error {
	return fmt.Errorf("not implemented: metrics report (阶段 4)")
}
