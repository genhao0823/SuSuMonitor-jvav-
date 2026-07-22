package com.susumonitor.server.module.metrics.controller;

import com.susumonitor.server.common.ApiResponse;
import com.susumonitor.server.common.vo.PageResult;
import com.susumonitor.server.module.metrics.service.MetricsService;
import com.susumonitor.server.module.metrics.vo.MetricsHistoryVo;
import com.susumonitor.server.module.metrics.vo.MetricsLatestVo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 提供固定宽表 Metrics 的最新值和历史分页查询接口。 */
@RestController
@RequestMapping("/api/servers")
@Validated
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    /** 查询服务器最新指标。 */
    @GetMapping("/{id}/metrics/latest")
    public ApiResponse<MetricsLatestVo> latest(@PathVariable("id") @Positive Long serverId) {
        return ApiResponse.success(metricsService.latest(serverId));
    }

    /** 查询服务器历史指标。 */
    @GetMapping("/{id}/metrics")
    public ApiResponse<PageResult<MetricsHistoryVo>> history(
            @PathVariable("id") @Positive Long serverId,
            @RequestParam("start_time") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam("end_time") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) Integer page,
            @RequestParam(value = "page_size", defaultValue = "20") @Min(1) @Max(100) Integer pageSize) {
        return ApiResponse.success(metricsService.history(serverId, startTime, endTime, page, pageSize));
    }
}
