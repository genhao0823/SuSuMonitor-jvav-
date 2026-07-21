// Package main 是 SuSuMonitor Agent 的启动入口。
//
// 阶段 0 只加载配置并打印启动信息，不做 WebSocket 连接和指标采集。
// 后续阶段逐步接入 wsclient、collector 和 reporter。
package main

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"os/signal"
	"syscall"

	"agent-go-SuMon/internal/config"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		fmt.Fprintf(os.Stderr, "config load failed: %v\n", err)
		os.Exit(1)
	}

	logger := newLogger(cfg.LogLevel)
	logger.Info("susumonitor agent starting",
		"backend_url", cfg.BackendURL,
		"server_id", cfg.ServerID,
		"token_prefix", cfg.TokenPrefix(),
		"collect_interval", cfg.CollectIntervalSeconds,
		"heartbeat_interval", cfg.HeartbeatIntervalSeconds,
	)

	// 阶段 0：等待退出信号；后续阶段在此启动 wsclient、collector 和 reporter。
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	logger.Info("agent running, waiting for signal to exit (阶段 0 骨架)")
	<-ctx.Done()
	logger.Info("agent shutting down")
}

// newLogger 创建结构化日志器，输出 JSON 到 stdout。
func newLogger(level string) *slog.Logger {
	var lvl slog.Level
	switch level {
	case "debug":
		lvl = slog.LevelDebug
	case "warn":
		lvl = slog.LevelWarn
	case "error":
		lvl = slog.LevelError
	default:
		lvl = slog.LevelInfo
	}
	handler := slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: lvl})
	return slog.New(handler)
}
