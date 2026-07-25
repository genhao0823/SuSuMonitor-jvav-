package com.susumonitor.server.module.metrics.mapper;

import com.susumonitor.server.module.metrics.entity.MetricsEntity;
import com.susumonitor.server.module.metrics.entity.MetricsIngestionEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 访问 Metrics 固定宽表，负责写入和 latest/history 查询。 */
@Mapper
public interface MetricsMapper {
    int insertMetric(@Param("metric") MetricsEntity metric);
    int insertIngestion(@Param("ingestion") MetricsIngestionEntity ingestion);
    LocalDateTime selectLatestCollectedAt(@Param("serverId") Long serverId);
    MetricsEntity selectLatestByServerId(@Param("serverId") Long serverId);
    List<MetricsEntity> selectHistory(@Param("serverId") Long serverId,
            @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime,
            @Param("offset") long offset, @Param("pageSize") int pageSize);
    long countHistory(@Param("serverId") Long serverId,
            @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
