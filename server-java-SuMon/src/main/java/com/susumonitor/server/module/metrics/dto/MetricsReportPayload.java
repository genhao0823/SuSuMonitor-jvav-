package com.susumonitor.server.module.metrics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Agent 单次指标上报载荷，与 Metrics 固定宽表字段一一对应。
 */
public class MetricsReportPayload {

    @JsonProperty("server_id")
    private Long serverId;
    @JsonProperty("collected_at")
    private OffsetDateTime collectedAt;
    @JsonProperty("cpu_percent")
    private BigDecimal cpuPercent;
    @JsonProperty("memory_percent")
    private BigDecimal memoryPercent;
    @JsonProperty("memory_used")
    private Long memoryUsed;
    @JsonProperty("memory_total")
    private Long memoryTotal;
    @JsonProperty("disk_percent")
    private BigDecimal diskPercent;
    @JsonProperty("disk_used")
    private Long diskUsed;
    @JsonProperty("disk_total")
    private Long diskTotal;
    @JsonProperty("net_rx")
    private Long netRx;
    @JsonProperty("net_tx")
    private Long netTx;
    private BigDecimal temperature;
    @JsonProperty("load_avg")
    private BigDecimal loadAvg;

    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    public OffsetDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(OffsetDateTime collectedAt) { this.collectedAt = collectedAt; }
    public BigDecimal getCpuPercent() { return cpuPercent; }
    public void setCpuPercent(BigDecimal cpuPercent) { this.cpuPercent = cpuPercent; }
    public BigDecimal getMemoryPercent() { return memoryPercent; }
    public void setMemoryPercent(BigDecimal memoryPercent) { this.memoryPercent = memoryPercent; }
    public Long getMemoryUsed() { return memoryUsed; }
    public void setMemoryUsed(Long memoryUsed) { this.memoryUsed = memoryUsed; }
    public Long getMemoryTotal() { return memoryTotal; }
    public void setMemoryTotal(Long memoryTotal) { this.memoryTotal = memoryTotal; }
    public BigDecimal getDiskPercent() { return diskPercent; }
    public void setDiskPercent(BigDecimal diskPercent) { this.diskPercent = diskPercent; }
    public Long getDiskUsed() { return diskUsed; }
    public void setDiskUsed(Long diskUsed) { this.diskUsed = diskUsed; }
    public Long getDiskTotal() { return diskTotal; }
    public void setDiskTotal(Long diskTotal) { this.diskTotal = diskTotal; }
    public Long getNetRx() { return netRx; }
    public void setNetRx(Long netRx) { this.netRx = netRx; }
    public Long getNetTx() { return netTx; }
    public void setNetTx(Long netTx) { this.netTx = netTx; }
    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
    public BigDecimal getLoadAvg() { return loadAvg; }
    public void setLoadAvg(BigDecimal loadAvg) { this.loadAvg = loadAvg; }
}
