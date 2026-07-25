package com.susumonitor.server.module.alert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 创建告警规则请求 DTO。
 *
 * <p>serverId 为 null 表示通用规则，匹配所有服务器。
 * metric 必须是 cpu/memory/disk/temperature/load 之一。
 * operator 必须是 >/>=/</<= 之一。
 * level 必须是 warning/critical 之一。</p>
 */
@Data
public class CreateAlertRuleRequest {

    // 服务器 ID，为 null 表示通用规则。
    private Long serverId;
    // 告警指标: cpu/memory/disk/temperature/load。
    @NotBlank(message = "metric must not be blank")
    private String metric;
    // 比较操作符: >/>=/</<=。
    @NotBlank(message = "operator must not be blank")
    private String operator;
    // 告警阈值，必须非负。
    @NotNull(message = "threshold value must not be null")
    @PositiveOrZero(message = "threshold value must be non-negative")
    private BigDecimal thresholdValue;
    // 告警等级: warning/critical。
    @NotBlank(message = "level must not be blank")
    private String level;
}
