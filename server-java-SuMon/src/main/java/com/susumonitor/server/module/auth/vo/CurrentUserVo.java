package com.susumonitor.server.module.auth.vo;

import java.time.OffsetDateTime;
import lombok.Data;

// 自动生成当前 VO 的 getter、setter、toString、equals 和 hashCode 方法。
@Data
public class CurrentUserVo {

    private Long id;

    private String username;

    private String role;

    private String reviewStatus;

    private OffsetDateTime reviewedAt;

    private OffsetDateTime createdAt;

}
