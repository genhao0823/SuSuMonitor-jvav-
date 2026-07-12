package com.susumonitor.server.module.server.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.Data;

// 自动生成当前 VO 的 getter、setter、toString、equals 和 hashCode 方法。
@Data
public class ServerStatusVo {

    private Long serverId;
    private String status;
    // 将 Java 的 agentStatus 属性映射为接口 JSON 字段 agent_status。
    @JsonProperty("agent_status")
    private String agentStatus;
    // 将 Java 的 lastHeartbeatAt 属性映射为接口 JSON 字段 last_heartbeat_at。
    @JsonProperty("last_heartbeat_at")
    private OffsetDateTime lastHeartbeatAt;
    private OffsetDateTime checkedAt;

}
