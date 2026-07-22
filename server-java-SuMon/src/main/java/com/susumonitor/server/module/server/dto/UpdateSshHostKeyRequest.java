package com.susumonitor.server.module.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 接收管理员通过可信带外渠道核对的 SSH 主机公钥指纹及显式轮换标记。
 */
// 自动生成请求字段访问方法，供 Spring MVC 反序列化和 Service 读取。
@Data
public class UpdateSshHostKeyRequest {

    // 要求管理员必须提供非空的预期指纹。
    @NotBlank
    // 只接受无填充 OpenSSH SHA-256 指纹，拒绝 MD5、SHA-1 和宽松格式。
    @Pattern(regexp = "^SHA256:[A-Za-z0-9+/]{43}$")
    // 将 Java 属性映射为接口 snake_case 字段。
    @JsonProperty("expected_fingerprint")
    private String expectedFingerprint;

    private boolean replace;
}
