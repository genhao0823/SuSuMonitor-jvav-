package com.susumonitor.server.module.metrics.service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 定义指标保留数据清理的调度业务契约。
 */
public interface MetricsCleanupService {

    /** 按配置的保留周期清理过期指标。 */
    Optional<CleanupResult> cleanupExpiredMetrics();

    /** 按指定时间边界清理过期指标，供验证场景固定边界。 */
    Optional<CleanupResult> cleanupExpiredMetrics(LocalDateTime cutoffTime);

    /**
     * 记录单轮清理的统计结果。
     *
     * @param cutoffTime 删除边界
     * @param batchCount 实际批次数
     * @param deletedRows 实际删除行数
     * @param durationMs 执行耗时毫秒数
     */
    record CleanupResult(LocalDateTime cutoffTime, int batchCount, int deletedRows, long durationMs) {
    }
}
