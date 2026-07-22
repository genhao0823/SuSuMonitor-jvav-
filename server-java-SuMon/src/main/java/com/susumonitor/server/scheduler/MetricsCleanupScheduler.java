package com.susumonitor.server.scheduler;

import com.susumonitor.server.module.metrics.service.MetricsCleanupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 按配置定时触发 Metrics 清理，不直接访问数据库 Mapper。
 */
@Slf4j
@Component
public class MetricsCleanupScheduler {

    private final MetricsCleanupService metricsCleanupService;

    /**
     * 构造 Metrics 清理调度器。
     *
     * @param metricsCleanupService Metrics 清理服务
     */
    public MetricsCleanupScheduler(MetricsCleanupService metricsCleanupService) {
        this.metricsCleanupService = metricsCleanupService;
    }

    /**
     * 执行一次 Metrics 过期清理；异常只影响当前轮次。
     */
    @Scheduled(cron = "${susumonitor.metrics.cleanup-cron}")
    public void cleanupExpiredMetrics() {
        try {
            metricsCleanupService.cleanupExpiredMetrics().ifPresentOrElse(
                    result -> log.info(
                            "Metrics cleanup completed, cutoffTime={}, batchCount={}, deletedRows={}",
                            result.cutoffTime(), result.batchCount(), result.deletedRows()),
                    () -> log.info("Metrics cleanup skipped because another run is active"));
        } catch (RuntimeException exception) {
            log.error("Metrics cleanup failed", exception);
        }
    }
}
