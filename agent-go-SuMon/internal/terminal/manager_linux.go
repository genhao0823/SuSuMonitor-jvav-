//go:build linux

package terminal

import (
	"context"
	"fmt"
	"io"
	"os"
	"os/exec"
	"sync"
	"syscall"
	"time"

	"github.com/creack/pty"
)

type manager struct {
	config    Config
	callbacks Callbacks
	mu        sync.Mutex
	sessions  map[string]*session
}

type session struct {
	id          string
	command     *exec.Cmd
	pty         *os.File
	outputQueue chan []byte
	done        chan struct{}
	waitDone    chan struct{}
	closeOnce   sync.Once
	mu          sync.Mutex
	lastInputAt time.Time
	openedAt    time.Time
}

// NewManager 创建 Linux PTY 管理器。
func NewManager(config Config, callbacks Callbacks) (Manager, error) {
	if !config.Enabled {
		return &manager{config: config, callbacks: callbacks, sessions: make(map[string]*session)}, nil
	}
	if config.Shell == "" || config.MaxSessions < 1 || config.MaxInputBytes < 1 || config.MaxOutputBytes < 1 ||
		config.OutputQueueSize < 1 || config.IdleTimeout <= 0 || config.MaxLifetime <= 0 {
		return nil, fmt.Errorf("invalid terminal configuration")
	}
	info, err := os.Stat(config.Shell)
	if err != nil || !info.Mode().IsRegular() || info.Mode()&0111 == 0 {
		return nil, fmt.Errorf("terminal shell is not executable")
	}
	return &manager{config: config, callbacks: callbacks, sessions: make(map[string]*session)}, nil
}

func (m *manager) Open(ctx context.Context, sessionID string, cols uint16, rows uint16) error {
	if !m.config.Enabled {
		return ErrUnsupported
	}
	if sessionID == "" || cols == 0 || rows == 0 {
		return fmt.Errorf("invalid terminal open request")
	}
	m.mu.Lock()
	if _, exists := m.sessions[sessionID]; exists {
		m.mu.Unlock()
		return nil
	}
	if len(m.sessions) >= m.config.MaxSessions {
		m.mu.Unlock()
		return ErrSessionLimit
	}
	command := exec.CommandContext(ctx, m.config.Shell)
	ptmx, err := pty.StartWithSize(command, &pty.Winsize{Cols: cols, Rows: rows})
	if err != nil {
		m.mu.Unlock()
		return fmt.Errorf("start pty: %w", err)
	}
	s := &session{id: sessionID, command: command, pty: ptmx, outputQueue: make(chan []byte, m.config.OutputQueueSize),
		done: make(chan struct{}), waitDone: make(chan struct{}), lastInputAt: time.Now(), openedAt: time.Now()}
	m.sessions[sessionID] = s
	m.mu.Unlock()
	go m.readOutput(s)
	go m.forwardOutput(s)
	go m.watchTimeouts(s)
	go m.waitProcess(s)
	return nil
}

func (m *manager) WriteInput(sessionID string, data []byte) error {
	if len(data) == 0 || len(data) > m.config.MaxInputBytes {
		return fmt.Errorf("invalid terminal input size")
	}
	s, err := m.find(sessionID)
	if err != nil {
		return err
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	if _, err = s.pty.Write(data); err != nil {
		return fmt.Errorf("write terminal input: %w", err)
	}
	s.lastInputAt = time.Now()
	return nil
}

func (m *manager) Resize(sessionID string, cols uint16, rows uint16) error {
	if cols == 0 || rows == 0 {
		return fmt.Errorf("invalid terminal size")
	}
	s, err := m.find(sessionID)
	if err != nil {
		return err
	}
	if err = pty.Setsize(s.pty, &pty.Winsize{Cols: cols, Rows: rows}); err != nil {
		return fmt.Errorf("resize terminal: %w", err)
	}
	return nil
}

func (m *manager) Close(sessionID string, reason string) error {
	s, err := m.find(sessionID)
	if err != nil {
		return err
	}
	m.closeSession(s, reason, nil)
	return nil
}

func (m *manager) CloseAll(reason string) {
	m.mu.Lock()
	sessions := make([]*session, 0, len(m.sessions))
	for _, s := range m.sessions {
		sessions = append(sessions, s)
	}
	m.mu.Unlock()
	for _, s := range sessions {
		m.closeSession(s, reason, nil)
	}
}

func (m *manager) find(sessionID string) (*session, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	s := m.sessions[sessionID]
	if s == nil {
		return nil, ErrSessionNotFound
	}
	return s, nil
}

func (m *manager) readOutput(s *session) {
	buffer := make([]byte, m.config.MaxOutputBytes)
	for {
		count, err := s.pty.Read(buffer)
		if count > 0 {
			chunk := append([]byte(nil), buffer[:count]...)
			select {
			case s.outputQueue <- chunk:
			case <-s.done:
				return
			default:
				m.closeSession(s, "output_queue_overflow", nil)
				return
			}
		}
		if err != nil {
			if err != io.EOF {
				m.closeSession(s, "pty_read_failed", nil)
			}
			return
		}
	}
}

func (m *manager) forwardOutput(s *session) {
	for {
		select {
		case data := <-s.outputQueue:
			if m.callbacks.Output != nil {
				m.callbacks.Output(s.id, data)
			}
		case <-s.done:
			return
		}
	}
}

func (m *manager) watchTimeouts(s *session) {
	ticker := time.NewTicker(time.Second)
	defer ticker.Stop()
	for {
		select {
		case <-ticker.C:
			s.mu.Lock()
			lastInputAt := s.lastInputAt
			openedAt := s.openedAt
			s.mu.Unlock()
			if time.Since(lastInputAt) >= m.config.IdleTimeout {
				m.closeSession(s, "idle_timeout", nil)
				return
			}
			if time.Since(openedAt) >= m.config.MaxLifetime {
				m.closeSession(s, "max_lifetime", nil)
				return
			}
		case <-s.done:
			return
		}
	}
}

func (m *manager) waitProcess(s *session) {
	defer close(s.waitDone)
	err := s.command.Wait()
	var exitCode *int
	if exitError, ok := err.(*exec.ExitError); ok {
		code := exitError.ExitCode()
		exitCode = &code
	}
	m.closeSession(s, "process_exited", exitCode)
}

func (m *manager) closeSession(s *session, reason string, exitCode *int) {
	s.closeOnce.Do(func() {
		close(s.done)
		if s.command.Process != nil {
			_ = syscall.Kill(-s.command.Process.Pid, syscall.SIGTERM)
			go func(processID int) {
				select {
				case <-s.waitDone:
				case <-time.After(5 * time.Second):
					_ = syscall.Kill(-processID, syscall.SIGKILL)
				}
			}(s.command.Process.Pid)
		}
		_ = s.pty.Close()
		m.mu.Lock()
		delete(m.sessions, s.id)
		m.mu.Unlock()
		if m.callbacks.Closed != nil {
			m.callbacks.Closed(s.id, reason, exitCode)
		}
	})
}
