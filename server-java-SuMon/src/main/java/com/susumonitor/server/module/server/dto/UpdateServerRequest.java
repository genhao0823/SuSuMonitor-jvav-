package com.susumonitor.server.module.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

// 自动生成当前 DTO 的 getter、setter、toString、equals 和 hashCode 方法。
@Data
public class UpdateServerRequest {

    // 校验服务器名称不能为空、不能是空字符串或只包含空白字符。
    @NotBlank
    // 限制服务器名称最大长度为 100 个字符。
    @Size(max = 100)
    private String name;

    // 校验服务器地址不能为空、不能是空字符串或只包含空白字符。
    @NotBlank
    // 限制服务器地址最大长度为 255 个字符。
    @Size(max = 255)
    private String host;

    // 限制服务器描述最大长度为 500 个字符，允许不填写。
    @Size(max = 500)
    private String description;

    // 校验 SSH 地址不能为空、不能是空字符串或只包含空白字符。
    @NotBlank
    // 限制 SSH 地址最大长度为 255 个字符。
    @Size(max = 255)
    // 将 Java 的 sshHost 属性映射为接口 JSON 字段 ssh_host。
    @JsonProperty("ssh_host")
    private String sshHost;

    // 校验 SSH 端口不能为 null。
    @NotNull
    // 限制 SSH 端口最小值为 1。
    @Min(1)
    // 限制 SSH 端口最大值为 65535。
    @Max(65535)
    // 将 Java 的 sshPort 属性映射为接口 JSON 字段 ssh_port。
    @JsonProperty("ssh_port")
    private Integer sshPort;

    // 校验 SSH 用户名不能为空、不能是空字符串或只包含空白字符。
    @NotBlank
    // 限制 SSH 用户名最大长度为 100 个字符。
    @Size(max = 100)
    // 将 Java 的 sshUser 属性映射为接口 JSON 字段 ssh_user。
    @JsonProperty("ssh_user")
    private String sshUser;

    // 校验 SSH 认证方式不能为空、不能是空字符串或只包含空白字符。
    @NotBlank
    // 限制 SSH 认证方式只能是 password 或 private_key。
    @Pattern(regexp = "^(password|private_key)$")
    // 将 Java 的 sshAuthType 属性映射为接口 JSON 字段 ssh_auth_type。
    @JsonProperty("ssh_auth_type")
    private String sshAuthType;

    // 将 Java 的 sshPassword 属性映射为接口 JSON 字段 ssh_password。
    @JsonProperty("ssh_password")
    // 防止 Lombok 生成的 toString 方法输出 SSH 密码。
    @ToString.Exclude
    private String sshPassword;

    // 将 Java 的 sshPrivateKey 属性映射为接口 JSON 字段 ssh_private_key。
    @JsonProperty("ssh_private_key")
    // 防止 Lombok 生成的 toString 方法输出 SSH 私钥。
    @ToString.Exclude
    private String sshPrivateKey;

    // 将 Java 的 sshPrivateKeyPassphrase 属性映射为接口 JSON 字段 ssh_private_key_passphrase。
    @JsonProperty("ssh_private_key_passphrase")
    // 防止 Lombok 生成的 toString 方法输出 SSH 私钥口令。
    @ToString.Exclude
    private String sshPrivateKeyPassphrase;
}
