package com.susumonitor.server.module.auth.vo;

import lombok.Data;
import lombok.ToString;

// 自动生成当前 VO 的 getter、setter、toString、equals 和 hashCode 方法。
@Data
public class LoginVo {

    // 防止 Lombok 生成的 toString 方法输出 JWT。
    @ToString.Exclude
    private String token;

    private String tokenType;

    private long expiresIn;

    private CurrentUserVo user;

}
