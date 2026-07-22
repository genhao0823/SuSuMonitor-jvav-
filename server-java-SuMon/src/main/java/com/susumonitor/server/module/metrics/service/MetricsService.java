package com.susumonitor.server.module.metrics.service;

import com.susumonitor.server.common.BusinessException;
import com.susumonitor.server.common.ErrorCode;
import com.susumonitor.server.common.vo.PageResult;
import com.susumonitor.server.module.metrics.dto.MetricsReportPayload;
import com.susumonitor.server.module.metrics.entity.MetricsEntity;
import com.susumonitor.server.module.metrics.mapper.MetricsMapper;
import com.susumonitor.server.module.metrics.vo.MetricsHistoryVo;
import com.susumonitor.server.module.metrics.vo.MetricsLatestVo;
import com.susumonitor.server.module.server.mapper.ServerMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 负责固定宽表指标校验、写入、最新值和历史分页查询。 */
@Service
public class MetricsService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_HISTORY_DAYS = 7;
    private static final ZoneId APPLICATION_ZONE = ZoneOffset.UTC;
    private final MetricsMapper metricsMapper;
    private final ServerMapper serverMapper;
    private final ApplicationEventPublisher eventPublisher;

    /** 注入 Metrics 和服务器数据访问组件。 */
    public MetricsService(MetricsMapper metricsMapper, ServerMapper serverMapper,
            ApplicationEventPublisher eventPublisher) {
        this.metricsMapper = metricsMapper;
        this.serverMapper = serverMapper;
        this.eventPublisher = eventPublisher;
    }

    /** 写入已认证 Agent 上报的一条完整宽表快照。 */
    @Transactional
    public void report(Long authenticatedServerId, MetricsReportPayload payload) {
        validatePayload(authenticatedServerId, payload);
        MetricsEntity entity = toEntity(payload);
        if (metricsMapper.insertMetric(entity) != 1) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR);
        }
        eventPublisher.publishEvent(new MetricsReportedEvent(toLatestVo(entity)));
    }

    /** 表示已写入数据库、等待事务提交后广播的指标事件。 */
    public record MetricsReportedEvent(MetricsLatestVo metrics) {
    }

    /** 查询服务器最新指标；无记录时返回资源不存在。 */
    @Transactional(readOnly = true)
    public MetricsLatestVo latest(Long serverId) {
        ensureServerExists(serverId);
        MetricsEntity entity = metricsMapper.selectLatestByServerId(serverId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return toLatestVo(entity);
    }

    /** 查询服务器时间窗口内的历史指标。 */
    @Transactional(readOnly = true)
    public PageResult<MetricsHistoryVo> history(Long serverId, OffsetDateTime startTime,
            OffsetDateTime endTime, Integer page, Integer pageSize) {
        ensureServerExists(serverId);
        if (page == null || page < 1 || pageSize == null || pageSize < 1 || pageSize > MAX_PAGE_SIZE
                || startTime == null || endTime == null || endTime.isBefore(startTime)
                || endTime.isAfter(startTime.plusDays(MAX_HISTORY_DAYS))) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }
        LocalDateTime start = startTime.atZoneSameInstant(APPLICATION_ZONE).toLocalDateTime();
        LocalDateTime end = endTime.atZoneSameInstant(APPLICATION_ZONE).toLocalDateTime();
        long offset = (long) (page - 1) * pageSize;
        List<MetricsHistoryVo> items = metricsMapper.selectHistory(serverId, start, end, offset, pageSize)
                .stream().map(this::toHistoryVo).toList();
        PageResult<MetricsHistoryVo> result = new PageResult<>();
        result.setItems(items);
        result.setTotal(metricsMapper.countHistory(serverId, start, end));
        result.setPage(page);
        result.setPageSize(pageSize);
        return result;
    }

    private void ensureServerExists(Long serverId) {
        if (serverId == null || serverId <= 0 || serverMapper.selectActiveServerById(serverId) == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private void validatePayload(Long authenticatedServerId, MetricsReportPayload payload) {
        if (payload == null || authenticatedServerId == null
                || !authenticatedServerId.equals(payload.getServerId())
                || payload.getCollectedAt() == null
                || payload.getCollectedAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5))) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }
        validatePercent(payload.getCpuPercent());
        validatePercent(payload.getMemoryPercent());
        validatePercent(payload.getDiskPercent());
        validateNonNegative(payload.getMemoryUsed());
        validateNonNegative(payload.getMemoryTotal());
        validateNonNegative(payload.getDiskUsed());
        validateNonNegative(payload.getDiskTotal());
        validateNonNegative(payload.getNetRx());
        validateNonNegative(payload.getNetTx());
        validateNonNegative(payload.getLoadAvg());
        if (payload.getMemoryUsed() != null && payload.getMemoryTotal() != null
                && payload.getMemoryUsed() > payload.getMemoryTotal()
                || payload.getDiskUsed() != null && payload.getDiskTotal() != null
                && payload.getDiskUsed() > payload.getDiskTotal()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }
    }

    private void validatePercent(BigDecimal value) {
        if (value != null && (value.signum() < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }
    }

    private void validateNonNegative(Number value) {
        if (value != null && value.doubleValue() < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }
    }

    private MetricsEntity toEntity(MetricsReportPayload payload) {
        MetricsEntity entity = new MetricsEntity();
        entity.setServerId(payload.getServerId());
        entity.setCpuPercent(payload.getCpuPercent());
        entity.setMemoryPercent(payload.getMemoryPercent());
        entity.setMemoryUsed(payload.getMemoryUsed());
        entity.setMemoryTotal(payload.getMemoryTotal());
        entity.setDiskPercent(payload.getDiskPercent());
        entity.setDiskUsed(payload.getDiskUsed());
        entity.setDiskTotal(payload.getDiskTotal());
        entity.setNetRx(payload.getNetRx());
        entity.setNetTx(payload.getNetTx());
        entity.setTemperature(payload.getTemperature());
        entity.setLoadAvg(payload.getLoadAvg());
        entity.setCollectedAt(payload.getCollectedAt().atZoneSameInstant(APPLICATION_ZONE).toLocalDateTime());
        return entity;
    }

    private MetricsLatestVo toLatestVo(MetricsEntity entity) {
        MetricsLatestVo result = new MetricsLatestVo();
        result.setServerId(entity.getServerId());
        result.setCpuPercent(entity.getCpuPercent());
        result.setMemoryPercent(entity.getMemoryPercent());
        result.setMemoryUsed(entity.getMemoryUsed());
        result.setMemoryTotal(entity.getMemoryTotal());
        result.setDiskPercent(entity.getDiskPercent());
        result.setDiskUsed(entity.getDiskUsed());
        result.setDiskTotal(entity.getDiskTotal());
        result.setNetRx(entity.getNetRx());
        result.setNetTx(entity.getNetTx());
        result.setTemperature(entity.getTemperature());
        result.setLoadAvg(entity.getLoadAvg());
        result.setCollectedAt(entity.getCollectedAt().atOffset(ZoneOffset.UTC));
        return result;
    }

    private MetricsHistoryVo toHistoryVo(MetricsEntity entity) {
        MetricsHistoryVo result = new MetricsHistoryVo();
        MetricsLatestVo latest = toLatestVo(entity);
        result.setServerId(latest.getServerId());
        result.setCpuPercent(latest.getCpuPercent());
        result.setMemoryPercent(latest.getMemoryPercent());
        result.setMemoryUsed(latest.getMemoryUsed());
        result.setMemoryTotal(latest.getMemoryTotal());
        result.setDiskPercent(latest.getDiskPercent());
        result.setDiskUsed(latest.getDiskUsed());
        result.setDiskTotal(latest.getDiskTotal());
        result.setNetRx(latest.getNetRx());
        result.setNetTx(latest.getNetTx());
        result.setTemperature(latest.getTemperature());
        result.setLoadAvg(latest.getLoadAvg());
        result.setCollectedAt(latest.getCollectedAt());
        return result;
    }
}
