package config

import "testing"

func setValidEnvironment(t *testing.T) {
	t.Helper()
	t.Setenv("SUSUMONITOR_BACKEND_URL", "ws://localhost:18080")
	t.Setenv("SUSUMONITOR_SERVER_ID", "42")
	t.Setenv("SUSUMONITOR_AGENT_TOKEN", "test-agent-token")
	t.Setenv("SUSUMONITOR_COLLECT_INTERVAL_SECONDS", "7")
	t.Setenv("SUSUMONITOR_HEARTBEAT_INTERVAL_SECONDS", "30")
	t.Setenv("SUSUMONITOR_RECONNECT_INITIAL_SECONDS", "5")
	t.Setenv("SUSUMONITOR_RECONNECT_MAX_SECONDS", "60")
	t.Setenv("SUSUMONITOR_LOG_LEVEL", "debug")
}

// TestLoadValidConfig 验证合法环境变量能完整加载并覆盖默认值。
func TestLoadValidConfig(t *testing.T) {
	setValidEnvironment(t)
	cfg, err := Load()
	if err != nil {
		t.Fatalf("Load() error = %v", err)
	}
	if cfg.ServerID != 42 || cfg.CollectIntervalSeconds != 7 || cfg.LogLevel != "debug" {
		t.Fatalf("unexpected config: %+v", cfg)
	}
}

func TestLoadRejectsInvalidInteger(t *testing.T) {
	setValidEnvironment(t)
	t.Setenv("SUSUMONITOR_HEARTBEAT_INTERVAL_SECONDS", "not-a-number")
	if _, err := Load(); err == nil {
		t.Fatal("Load() error = nil, want invalid integer error")
	}
}

func TestLoadRejectsInvalidValues(t *testing.T) {
	tests := []struct {
		name  string
		key   string
		value string
	}{
		{"missing backend URL", "SUSUMONITOR_BACKEND_URL", ""},
		{"invalid backend URL", "SUSUMONITOR_BACKEND_URL", "http://localhost:18080"},
		{"missing token", "SUSUMONITOR_AGENT_TOKEN", ""},
		{"zero collect interval", "SUSUMONITOR_COLLECT_INTERVAL_SECONDS", "0"},
		{"zero heartbeat interval", "SUSUMONITOR_HEARTBEAT_INTERVAL_SECONDS", "0"},
		{"zero reconnect initial", "SUSUMONITOR_RECONNECT_INITIAL_SECONDS", "0"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			setValidEnvironment(t)
			t.Setenv(tt.key, tt.value)
			if _, err := Load(); err == nil {
				t.Fatal("Load() error = nil, want validation error")
			}
		})
	}
}

func TestLoadRejectsReconnectMaxBelowInitial(t *testing.T) {
	setValidEnvironment(t)
	t.Setenv("SUSUMONITOR_RECONNECT_INITIAL_SECONDS", "10")
	t.Setenv("SUSUMONITOR_RECONNECT_MAX_SECONDS", "5")
	if _, err := Load(); err == nil {
		t.Fatal("Load() error = nil, want reconnect range error")
	}
}
