package com.susumonitor.server.module.metrics.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/** 对外返回单个服务器最新固定宽表指标。 */
@Data
public class MetricsLatestVo {
    @JsonProperty("server_id") private Long serverId;
    @JsonProperty("cpu_percent") private BigDecimal cpuPercent;
    @JsonProperty("memory_percent") private BigDecimal memoryPercent;
    @JsonProperty("memory_used") private Long memoryUsed;
    @JsonProperty("memory_total") private Long memoryTotal;
    @JsonProperty("disk_percent") private BigDecimal diskPercent;
    @JsonProperty("disk_used") private Long diskUsed;
    @JsonProperty("disk_total") private Long diskTotal;
    @JsonProperty("net_rx") private Long netRx;
    @JsonProperty("net_tx") private Long netTx;
    private BigDecimal temperature;
    @JsonProperty("load_avg") private BigDecimal loadAvg;
    @JsonProperty("collected_at") private OffsetDateTime collectedAt;
}
