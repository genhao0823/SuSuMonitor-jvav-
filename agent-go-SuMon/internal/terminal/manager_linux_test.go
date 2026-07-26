//go:build linux

package terminal

import (
	"context"
	"os"
	"testing"
	"time"
)

func TestManagerOpenInputOutputAndClose(t *testing.T) {
	if _, err := os.Stat("/bin/bash"); err != nil {
		t.Skip("/bin/bash is unavailable")
	}
	output := make(chan []byte, 1)
	closed := make(chan string, 1)
	manager, err := NewManager(testConfig(), Callbacks{
		Output: func(sessionID string, data []byte) { output <- data },
		Closed: func(sessionID string, reason string, exitCode *int) { closed <- reason },
	})
	if err != nil {
		t.Fatalf("NewManager() error = %v", err)
	}
	if err = manager.Open(context.Background(), "session-1", 80, 24); err != nil {
		t.Fatalf("Open() error = %v", err)
	}
	if err = manager.WriteInput("session-1", []byte("printf terminal-test\\n")); err != nil {
		t.Fatalf("WriteInput() error = %v", err)
	}
	select {
	case data := <-output:
		if len(data) == 0 {
			t.Fatal("empty terminal output")
		}
	case <-time.After(5 * time.Second):
		t.Fatal("terminal output timeout")
	}
	if err = manager.Resize("session-1", 100, 30); err != nil {
		t.Fatalf("Resize() error = %v", err)
	}
	if err = manager.Close("session-1", "test_close"); err != nil {
		t.Fatalf("Close() error = %v", err)
	}
	select {
	case reason := <-closed:
		if reason != "test_close" {
			t.Fatalf("close reason = %q", reason)
		}
	case <-time.After(5 * time.Second):
		t.Fatal("terminal close timeout")
	}
}

func TestManagerLimitsSessions(t *testing.T) {
	if _, err := os.Stat("/bin/bash"); err != nil {
		t.Skip("/bin/bash is unavailable")
	}
	config := testConfig()
	config.MaxSessions = 1
	manager, err := NewManager(config, Callbacks{})
	if err != nil {
		t.Fatalf("NewManager() error = %v", err)
	}
	if err = manager.Open(context.Background(), "session-1", 80, 24); err != nil {
		t.Fatalf("Open() error = %v", err)
	}
	if err = manager.Open(context.Background(), "session-2", 80, 24); err != ErrSessionLimit {
		t.Fatalf("Open() error = %v, want ErrSessionLimit", err)
	}
	manager.CloseAll("test_done")
}

func testConfig() Config {
	return Config{Enabled: true, Shell: "/bin/bash", MaxSessions: 4, MaxInputBytes: 16 * 1024,
		MaxOutputBytes: 16 * 1024, OutputQueueSize: 8, IdleTimeout: time.Minute, MaxLifetime: time.Minute}
}
