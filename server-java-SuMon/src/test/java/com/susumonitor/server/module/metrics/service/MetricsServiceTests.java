package com.susumonitor.server.module.metrics.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.susumonitor.server.common.BusinessException;
import com.susumonitor.server.common.ErrorCode;
import com.susumonitor.server.module.metrics.dto.MetricsReportPayload;
import com.susumonitor.server.module.metrics.mapper.MetricsMapper;
import com.susumonitor.server.module.server.entity.ServerEntity;
import com.susumonitor.server.module.server.mapper.ServerMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;

/** 验证 Metrics 至少一次投递下的 UUID 幂等和严格时间有序规则。 */
@ExtendWith(MockitoExtension.class)
class MetricsServiceTests {

    private static final Long SERVER_ID = 1L;
    private static final OffsetDateTime COLLECTED_AT = OffsetDateTime.of(
            2026, 7, 25, 12, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private MetricsMapper metricsMapper;

    @Mock
    private ServerMapper serverMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MetricsService service;

    /** 创建每个用例独立的 Service，并默认模拟已锁定的有效服务器行。 */
    @BeforeEach
    void setUp() {
        service = new MetricsServiceImpl(metricsMapper, serverMapper, eventPublisher);
    }

    /** 首次投递写入去重记录和指标，并发布一次 Metrics 事件。 */
    @Test
    void firstReportShouldPersistAndPublishEvent() {
        lockActiveServer();
        when(metricsMapper.insertIngestion(any())).thenReturn(1);
        when(metricsMapper.insertMetric(any())).thenReturn(1);

        service.report(SERVER_ID, UUID.randomUUID().toString(), payload(COLLECTED_AT));

        verify(metricsMapper).insertMetric(any());
        verify(eventPublisher).publishEvent(any(MetricsService.MetricsReportedEvent.class));
    }

    /** 相同消息 ID 的数据库唯一键冲突视为成功重试，不再写指标或发布事件。 */
    @Test
    void duplicateMessageIdShouldBeSilentlyIdempotent() {
        lockActiveServer();
        when(metricsMapper.insertIngestion(any())).thenThrow(new DuplicateKeyException("duplicate"));

        assertDoesNotThrow(() -> service.report(SERVER_ID, UUID.randomUUID().toString(), payload(COLLECTED_AT)));

        verify(metricsMapper, never()).selectLatestCollectedAt(any());
        verify(metricsMapper, never()).insertMetric(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    /** 与最后接受采样时间相同的不同消息必须拒绝，维持采样时间严格递增。 */
    @Test
    void equalCollectedAtShouldBeRejectedAsOutOfOrder() {
        lockActiveServer();
        LocalDateTime latest = COLLECTED_AT.toLocalDateTime();
        when(metricsMapper.insertIngestion(any())).thenReturn(1);
        when(metricsMapper.selectLatestCollectedAt(SERVER_ID)).thenReturn(latest);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.report(SERVER_ID, UUID.randomUUID().toString(), payload(COLLECTED_AT)));

        org.junit.jupiter.api.Assertions.assertEquals(ErrorCode.INVALID_REQUEST_PARAMETER, exception.getErrorCode());
        verify(metricsMapper, never()).insertMetric(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    /** 早于最后接受采样的消息必须拒绝，不允许旧值回退 latest 和告警状态。 */
    @Test
    void olderCollectedAtShouldBeRejectedAsOutOfOrder() {
        lockActiveServer();
        when(metricsMapper.insertIngestion(any())).thenReturn(1);
        when(metricsMapper.selectLatestCollectedAt(SERVER_ID))
                .thenReturn(COLLECTED_AT.plusSeconds(1).toLocalDateTime());

        assertThrows(BusinessException.class,
                () -> service.report(SERVER_ID, UUID.randomUUID().toString(), payload(COLLECTED_AT)));

        verify(metricsMapper, never()).insertMetric(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    /** 指标写入失败时向上抛出数据库错误；真实事务会同时回滚本次 ingestion 登记。 */
    @Test
    void failedMetricInsertShouldRaiseDatabaseError() {
        lockActiveServer();
        when(metricsMapper.insertIngestion(any())).thenReturn(1);
        when(metricsMapper.insertMetric(any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.report(SERVER_ID, UUID.randomUUID().toString(), payload(COLLECTED_AT)));

        org.junit.jupiter.api.Assertions.assertEquals(ErrorCode.DATABASE_ERROR, exception.getErrorCode());
        verify(eventPublisher, never()).publishEvent(any());
    }

    /** 未提供合法 UUID 时在进入数据库前拒绝，避免无法建立可靠幂等键。 */
    @Test
    void missingMessageIdShouldBeRejectedBeforeDatabaseAccess() {
        assertThrows(BusinessException.class, () -> service.report(SERVER_ID, null, payload(COLLECTED_AT)));

        verify(serverMapper, never()).selectActiveServerForUpdateById(eq(SERVER_ID));
        verify(metricsMapper, never()).insertIngestion(any());
    }

    /** 构造满足宽表校验的最小 Metrics 上报载荷。 */
    private MetricsReportPayload payload(OffsetDateTime collectedAt) {
        MetricsReportPayload payload = new MetricsReportPayload();
        payload.setServerId(SERVER_ID);
        payload.setCollectedAt(collectedAt);
        payload.setCpuPercent(BigDecimal.TEN);
        return payload;
    }

    /** 模拟数据库已锁定目标服务器，隔离本类对指标有序写入规则的验证。 */
    private void lockActiveServer() {
        when(serverMapper.selectActiveServerForUpdateById(SERVER_ID)).thenReturn(new ServerEntity());
    }
}
