package com.susumonitor.server.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

// 自动生成当前 DTO 的 getter、setter、toString、equals 和 hashCode 方法。
@Data
public class LoginRequest {

    // 校验登录用户名不能为空、不能是空字符串或只包含空白字符。
    @NotBlank
    private String username;

    // 校验登录密码不能为空、不能是空字符串或只包含空白字符。
    @NotBlank
    // 防止 Lombok 生成的 toString 方法输出用户密码。
    @ToString.Exclude
    private String password;
}
