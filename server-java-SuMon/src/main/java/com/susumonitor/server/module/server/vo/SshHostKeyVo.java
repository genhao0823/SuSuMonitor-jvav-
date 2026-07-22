package com.susumonitor.server.module.server.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * 返回已确认 SSH 主机公钥的非敏感信息和本次操作结果。
 */
// 自动生成响应字段访问方法，并由 Jackson 按全局 snake_case 规则序列化。
@Data
public class SshHostKeyVo {

    // 将 Java 属性映射为接口 JSON 字段 server_id。
    @JsonProperty("server_id")
    private Long serverId;
    // 将 Java 属性映射为接口 JSON 字段 host_key_algorithm。
    @JsonProperty("host_key_algorithm")
    private String hostKeyAlgorithm;
    // 将 Java 属性映射为接口 JSON 字段 host_key_fingerprint。
    @JsonProperty("host_key_fingerprint")
    private String hostKeyFingerprint;
    private String operation;
    // 将 Java 属性映射为接口 JSON 字段 verified_at。
    @JsonProperty("verified_at")
    private OffsetDateTime verifiedAt;
}
