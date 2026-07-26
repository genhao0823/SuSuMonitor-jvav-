package com.susumonitor.server.module.alert.service;

import com.susumonitor.server.module.metrics.service.MetricsService.MetricsReportedEvent;

/**
 * 定义事务提交后指标告警评估的事件处理契约。
 */
public interface AlertEvaluationService {

    /** 消费已提交的指标事件并评估对应告警规则。 */
    void onMetricsReported(MetricsReportedEvent event);
}
