package com.susumonitor.server.module.server.vo;

import java.time.OffsetDateTime;
import lombok.Data;

// 自动生成当前 VO 的 getter、setter、toString、equals 和 hashCode 方法。
@Data
public class SshTestVo {

    private Long serverId;
    private boolean success;
    private OffsetDateTime testedAt;

}
