// Package main 是 SuSuMonitor Agent 的启动入口。
//
// 阶段 2：加载配置并启动 WebSocket 客户端，实现连接、鉴权、心跳和重连。
// 指标采集和上报在阶段 3-4 接入。
package main

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"os"
	"os/signal"
	"syscall"
	"time"

	"agent-go-SuMon/internal/collector"
	"agent-go-SuMon/internal/config"
	"agent-go-SuMon/internal/reporter"
	"agent-go-SuMon/internal/wsclient"
)

// main 加载配置并运行 Agent，配置错误或连接运行异常时以非零状态退出。
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

	client := wsclient.NewClient(
		cfg.BackendURL,
		cfg.ServerID,
		cfg.AgentToken,
		logger,
		time.Duration(cfg.HeartbeatIntervalSeconds)*time.Second,
		time.Duration(cfg.ReconnectInitialSeconds)*time.Second,
		time.Duration(cfg.ReconnectMaxSeconds)*time.Second,
	)

	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	if err := run(ctx, cfg, logger, client); err != nil {
		logger.Error("agent exited with error", "error", err)
		os.Exit(1)
	}
	logger.Info("agent shutdown complete")
}

// run 在同一可取消生命周期内运行 WebSocket、指标采集和上报。
func run(ctx context.Context, cfg *config.Config, logger *slog.Logger, client *wsclient.Client) error {
	metricsCollector := collector.NewGopsutilCollector()
	metricsReporter := reporter.NewReporter(cfg.ServerID, logger, client)
	collectTicker := time.NewTicker(time.Duration(cfg.CollectIntervalSeconds) * time.Second)
	defer collectTicker.Stop()

	clientErrCh := make(chan error, 1)
	go func() {
		clientErrCh <- client.Run(ctx)
	}()

	for {
		select {
		case <-ctx.Done():
			err := <-clientErrCh
			if errors.Is(err, context.Canceled) {
				return nil
			}
			return err
		case err := <-clientErrCh:
			if errors.Is(err, context.Canceled) {
				return nil
			}
			return err
		case <-collectTicker.C:
			metrics, err := metricsCollector.Collect()
			if err != nil {
				logger.Warn("metrics collection failed", "error", err)
				continue
			}
			if err := metricsReporter.Report(metrics); err != nil {
				logger.Warn("metrics report failed", "error", err)
			}
		}
	}
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
