package com.susumonitor.server.module.server.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.Data;

// 自动生成当前 VO 的 getter、setter、toString、equals 和 hashCode 方法。
@Data
public class ServerVo {

    private Long id;
    private String name;
    private String host;
    private String description;
    private String status;
    // 将 Java 的 sshHost 属性映射为接口 JSON 字段 ssh_host。
    @JsonProperty("ssh_host")
    private String sshHost;
    // 将 Java 的 sshPort 属性映射为接口 JSON 字段 ssh_port。
    @JsonProperty("ssh_port")
    private Integer sshPort;
    // 将 Java 的 sshUser 属性映射为接口 JSON 字段 ssh_user。
    @JsonProperty("ssh_user")
    private String sshUser;
    // 将 Java 的 sshAuthType 属性映射为接口 JSON 字段 ssh_auth_type。
    @JsonProperty("ssh_auth_type")
    private String sshAuthType;
    // 将 Java 的 agentId 属性映射为接口 JSON 字段 agent_id。
    @JsonProperty("agent_id")
    private String agentId;
    // 将 Java 的 agentStatus 属性映射为接口 JSON 字段 agent_status。
    @JsonProperty("agent_status")
    private String agentStatus;
    // 将 Java 的 lastHeartbeatAt 属性映射为接口 JSON 字段 last_heartbeat_at。
    @JsonProperty("last_heartbeat_at")
    private OffsetDateTime lastHeartbeatAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
