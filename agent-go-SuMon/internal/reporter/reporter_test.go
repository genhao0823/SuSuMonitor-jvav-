package reporter

import (
	"log/slog"
	"testing"

	"agent-go-SuMon/internal/collector"
	"agent-go-SuMon/internal/wsclient"
)

type recordingSender struct {
	payload wsclient.MetricsPayload
}

func (s *recordingSender) SendMetrics(payload wsclient.MetricsPayload) error {
	s.payload = payload
	return nil
}

// TestReportMapsMetrics 验证采集快照到协议 Payload 的字段映射。
func TestReportMapsMetrics(t *testing.T) {
	cpu := 35.5
	sender := &recordingSender{}
	reporter := NewReporter(42, slog.Default(), sender)
	err := reporter.Report(collector.Metrics{CPUPercent: &cpu})
	if err != nil {
		t.Fatalf("Report() error = %v", err)
	}
	if sender.payload.ServerID != 42 {
		t.Errorf("server_id = %d, want 42", sender.payload.ServerID)
	}
	if sender.payload.CPUPercent == nil || *sender.payload.CPUPercent != 35.5 {
		t.Errorf("cpu_percent = %v, want 35.5", sender.payload.CPUPercent)
	}
	if sender.payload.CollectedAt == "" {
		t.Error("collected_at is empty")
	}
}
