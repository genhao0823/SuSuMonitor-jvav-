package collector

import (
	"errors"
	"testing"
	"time"

	"github.com/shirou/gopsutil/v3/disk"
	"github.com/shirou/gopsutil/v3/load"
	"github.com/shirou/gopsutil/v3/mem"
	"github.com/shirou/gopsutil/v3/net"
)

func testCollector() *GopsutilCollector {
	return &GopsutilCollector{
		cpuPercent: func(time.Duration, bool) ([]float64, error) { return []float64{35.236}, nil },
		virtualMemory: func() (*mem.VirtualMemoryStat, error) {
			return &mem.VirtualMemoryStat{UsedPercent: 48.126, Used: 481, Total: 1000}, nil
		},
		diskUsage: func(path string) (*disk.UsageStat, error) {
			if path != systemDiskPath() {
				return nil, errors.New("unexpected disk path")
			}
			return &disk.UsageStat{UsedPercent: 61.456, Used: 614, Total: 1000}, nil
		},
		netIOCounters: func(bool) ([]net.IOCountersStat, error) {
			return []net.IOCountersStat{{BytesRecv: 123, BytesSent: 456}}, nil
		},
		sensorsTemperatures: func() ([]sensorTemperature, error) {
			return []sensorTemperature{{Temperature: 42.567}}, nil
		},
		loadAvg: func() (*load.AvgStat, error) { return &load.AvgStat{Load1: 0.756}, nil },
	}
}

func TestGopsutilCollectorCollect(t *testing.T) {
	metrics, err := testCollector().Collect()
	if err != nil {
		t.Fatalf("Collect() error = %v", err)
	}
	if *metrics.CPUPercent != 35.24 || *metrics.MemoryPercent != 48.13 || *metrics.DiskPercent != 61.46 {
		t.Fatalf("percentages were not rounded: %+v", metrics)
	}
	if *metrics.MemoryUsed != 481 || *metrics.MemoryTotal != 1000 || *metrics.DiskUsed != 614 || *metrics.DiskTotal != 1000 {
		t.Fatalf("size fields mismatch: %+v", metrics)
	}
	if *metrics.NetRx != 123 || *metrics.NetTx != 456 || *metrics.Temperature != 42.57 || *metrics.LoadAvg != 0.76 {
		t.Fatalf("optional or network fields mismatch: %+v", metrics)
	}
}

func TestGopsutilCollectorOptionalMetricsMayBeNil(t *testing.T) {
	c := testCollector()
	c.sensorsTemperatures = func() ([]sensorTemperature, error) { return nil, errors.New("unsupported") }
	c.loadAvg = func() (*load.AvgStat, error) { return nil, errors.New("unsupported") }
	metrics, err := c.Collect()
	if err != nil {
		t.Fatalf("Collect() error = %v", err)
	}
	if metrics.Temperature != nil || metrics.LoadAvg != nil {
		t.Fatalf("optional metrics should be nil: %+v", metrics)
	}
}

func TestGopsutilCollectorRequiredMetricErrors(t *testing.T) {
	tests := []struct {
		name string
		set  func(*GopsutilCollector)
	}{
		{"cpu", func(c *GopsutilCollector) {
			c.cpuPercent = func(time.Duration, bool) ([]float64, error) { return nil, errors.New("cpu") }
		}},
		{"memory", func(c *GopsutilCollector) {
			c.virtualMemory = func() (*mem.VirtualMemoryStat, error) { return nil, errors.New("memory") }
		}},
		{"disk", func(c *GopsutilCollector) {
			c.diskUsage = func(string) (*disk.UsageStat, error) { return nil, errors.New("disk") }
		}},
		{"network", func(c *GopsutilCollector) {
			c.netIOCounters = func(bool) ([]net.IOCountersStat, error) { return nil, errors.New("network") }
		}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := testCollector()
			tt.set(c)
			if _, err := c.Collect(); err == nil {
				t.Fatalf("Collect() error = nil, want error")
			}
		})
	}
}
