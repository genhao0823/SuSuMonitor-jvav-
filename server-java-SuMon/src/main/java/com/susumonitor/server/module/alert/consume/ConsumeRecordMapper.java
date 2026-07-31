package com.susumonitor.server.module.alert.consume;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 消费幂等记录数据访问，维护消费者对事件的去重与失败留痕。
 */
@Mapper
public interface ConsumeRecordMapper {

    /** 插入已消费记录，回写主键（consumer+event_id 唯一键保证幂等）。 */
    int insert(@Param("record") ConsumeRecordEntity record);

    /** 查询该消费者是否已成功消费过该事件。 */
    boolean existsConsumed(@Param("consumer") String consumer, @Param("eventId") String eventId);

    /** 失败留痕：更新状态为 failed 并记录尝试次数与原因。 */
    int markFailed(@Param("id") Long id, @Param("attempts") int attempts, @Param("lastError") String lastError);
}
