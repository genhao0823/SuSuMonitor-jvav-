// Package reporter 构造并发送 metrics.report 消息。
//
// 阶段 0 只定义结构；阶段 4 将实现消息构造和上报。
package reporter

import (
	"fmt"
	"log/slog"
)

// Reporter 负责将采集的指标构造为 metrics.report 消息并通过连接发送。
type Reporter struct {
	serverID int64
	logger   *slog.Logger
	// 阶段 4 补充：sender wsclient.Sender
}

// NewReporter 创建 Reporter。
func NewReporter(serverID int64, logger *slog.Logger) *Reporter {
	return &Reporter{
		serverID: serverID,
		logger:   logger,
	}
}

// Report 将采集的指标构造为 metrics.report 消息并发送。
//
// 阶段 0 未实现；阶段 4 接入 collector 和 wsclient 后实现。
func (r *Reporter) Report(metrics interface{}) error {
	return fmt.Errorf("not implemented: metrics report (阶段 4)")
}
