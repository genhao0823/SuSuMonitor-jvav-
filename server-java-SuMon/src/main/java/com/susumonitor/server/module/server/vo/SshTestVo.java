package com.susumonitor.server.module.server.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * 返回严格校验主机身份并完成凭据认证后的 SSH 连接测试结果。
 */
// 自动生成当前 VO 的 getter、setter、toString、equals 和 hashCode 方法。
@Data
public class SshTestVo {

    // 将 Java 属性映射为接口 JSON 字段 server_id。
    @JsonProperty("server_id")
    private Long serverId;
    private boolean connected;
    // 将 Java 属性映射为接口 JSON 字段 host_key_algorithm。
    @JsonProperty("host_key_algorithm")
    private String hostKeyAlgorithm;
    // 将 Java 属性映射为接口 JSON 字段 host_key_fingerprint。
    @JsonProperty("host_key_fingerprint")
    private String hostKeyFingerprint;
    // 将 Java 属性映射为接口 JSON 字段 auth_type。
    @JsonProperty("auth_type")
    private String authType;
    // 将 Java 属性映射为接口 JSON 字段 duration_ms。
    @JsonProperty("duration_ms")
    private long durationMs;
    // 将 Java 属性映射为接口 JSON 字段 tested_at。
    @JsonProperty("tested_at")
    private OffsetDateTime testedAt;

}
