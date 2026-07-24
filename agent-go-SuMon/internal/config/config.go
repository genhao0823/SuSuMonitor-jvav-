// Package config 定义 SuSuMonitor Agent 的运行时配置。
//
// 配置来源优先级：环境变量 > 配置文件 > 默认值。
// 阶段 0 只实现环境变量加载；配置文件解析在后续阶段补充。
package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
)

// Config 是 Agent 的运行时配置。
type Config struct {
	// BackendURL 是后端 WebSocket 地址，如 ws://localhost:18080。
	BackendURL string
	// ServerID 是 admin 预建的服务器 ID。
	ServerID int64
	// AgentToken 是 admin 通过 REST 预发放的 Agent Token，明文仅一次性返回。
	AgentToken string
	// CollectIntervalSeconds 是指标采集间隔，默认 5 秒。
	CollectIntervalSeconds int
	// HeartbeatIntervalSeconds 是心跳间隔，默认 30 秒。
	HeartbeatIntervalSeconds int
	// ReconnectInitialSeconds 是重连初始间隔，默认 5 秒。
	ReconnectInitialSeconds int
	// ReconnectMaxSeconds 是重连最大间隔，默认 60 秒。
	ReconnectMaxSeconds int
	// LogLevel 是日志级别，如 info、debug。
	LogLevel string
}

// Load 从环境变量加载配置并校验。
//
// 期望的环境变量：
//   - SUSUMONITOR_BACKEND_URL
//   - SUSUMONITOR_SERVER_ID
//   - SUSUMONITOR_AGENT_TOKEN
//   - SUSUMONITOR_COLLECT_INTERVAL_SECONDS
//   - SUSUMONITOR_HEARTBEAT_INTERVAL_SECONDS
//   - SUSUMONITOR_RECONNECT_INITIAL_SECONDS
//   - SUSUMONITOR_RECONNECT_MAX_SECONDS
//   - SUSUMONITOR_LOG_LEVEL
func Load() (*Config, error) {
	cfg := &Config{
		BackendURL:              os.Getenv("SUSUMONITOR_BACKEND_URL"),
		AgentToken:              os.Getenv("SUSUMONITOR_AGENT_TOKEN"),
		LogLevel:                getenvDefault("SUSUMONITOR_LOG_LEVEL", "info"),
		CollectIntervalSeconds:  getenvIntDefault("SUSUMONITOR_COLLECT_INTERVAL_SECONDS", 5),
		HeartbeatIntervalSeconds: getenvIntDefault("SUSUMONITOR_HEARTBEAT_INTERVAL_SECONDS", 30),
		ReconnectInitialSeconds: getenvIntDefault("SUSUMONITOR_RECONNECT_INITIAL_SECONDS", 5),
		ReconnectMaxSeconds:     getenvIntDefault("SUSUMONITOR_RECONNECT_MAX_SECONDS", 60),
	}

	serverID, err := getenvInt64("SUSUMONITOR_SERVER_ID")
	if err != nil {
		return nil, fmt.Errorf("server_id: %w", err)
	}
	cfg.ServerID = serverID

	if err := cfg.validate(); err != nil {
		return nil, err
	}
	return cfg, nil
}

// validate 校验配置项的合法性，非法配置启动失败。
func (c *Config) validate() error {
	if c.BackendURL == "" {
		return fmt.Errorf("SUSUMONITOR_BACKEND_URL is required")
	}
	if !strings.HasPrefix(c.BackendURL, "ws://") && !strings.HasPrefix(c.BackendURL, "wss://") {
		return fmt.Errorf("SUSUMONITOR_BACKEND_URL must start with ws:// or wss://")
	}
	if c.AgentToken == "" {
		return fmt.Errorf("SUSUMONITOR_AGENT_TOKEN is required")
	}
	if c.CollectIntervalSeconds < 1 {
		return fmt.Errorf("collect interval must be >= 1, got %d", c.CollectIntervalSeconds)
	}
	if c.HeartbeatIntervalSeconds < 1 {
		return fmt.Errorf("heartbeat interval must be >= 1, got %d", c.HeartbeatIntervalSeconds)
	}
	if c.ReconnectInitialSeconds < 1 {
		return fmt.Errorf("reconnect initial interval must be >= 1, got %d", c.ReconnectInitialSeconds)
	}
	if c.ReconnectMaxSeconds < c.ReconnectInitialSeconds {
		return fmt.Errorf("reconnect max (%d) must be >= initial (%d)",
			c.ReconnectMaxSeconds, c.ReconnectInitialSeconds)
	}
	return nil
}

// TokenPrefix 返回 agent_token 的前 8 字符，用于日志脱敏。
//
// 完整 token 不得输出到日志。
func (c *Config) TokenPrefix() string {
	if len(c.AgentToken) <= 8 {
		return c.AgentToken
	}
	return c.AgentToken[:8]
}

// getenvDefault 读取环境变量，为空时返回默认值。
func getenvDefault(key, def string) string {
	v := os.Getenv(key)
	if v == "" {
		return def
	}
	return v
}

// getenvIntDefault 读取环境变量为 int，为空时返回默认值。
func getenvIntDefault(key string, def int) int {
	v := os.Getenv(key)
	if v == "" {
		return def
	}
	n, err := strconv.Atoi(v)
	if err != nil {
		return def
	}
	return n
}

// getenvInt64 读取环境变量为 int64，为空或非法时返回错误。
func getenvInt64(key string) (int64, error) {
	v := os.Getenv(key)
	if v == "" {
		return 0, fmt.Errorf("%s is required", key)
	}
	n, err := strconv.ParseInt(v, 10, 64)
	if err != nil {
		return 0, fmt.Errorf("%s must be an integer: %s", key, v)
	}
	if n <= 0 {
		return 0, fmt.Errorf("%s must be positive: %d", key, n)
	}
	return n, nil
}
