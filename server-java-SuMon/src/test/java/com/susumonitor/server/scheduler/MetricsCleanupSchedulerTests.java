package com.susumonitor.server.scheduler;

import static org.mockito.Mockito.verify;

import com.susumonitor.server.module.metrics.service.MetricsCleanupService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * 验证 Metrics Scheduler 只负责触发清理服务并处理结果。
 */
class MetricsCleanupSchedulerTests {

    /** 验证调度器会调用清理服务。 */
    @Test
    void schedulerShouldTriggerCleanupService() {
        MetricsCleanupService service = Mockito.mock(MetricsCleanupService.class);
        Mockito.when(service.cleanupExpiredMetrics()).thenReturn(Optional.empty());
        MetricsCleanupScheduler scheduler = new MetricsCleanupScheduler(service);

        scheduler.cleanupExpiredMetrics();

        verify(service).cleanupExpiredMetrics();
    }
}
