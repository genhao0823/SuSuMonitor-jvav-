package com.susumonitor.server.module.alert.mapper;

import com.susumonitor.server.module.alert.entity.AlertStateEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 告警状态 Mapper，维护每条规则在每台服务器上的当前越界状态。
 *
 * <p>乐观锁通过 SQL WHERE version = #{version} 手动处理，
 * 不使用 MyBatis-Plus 的 @Version 注解，因为项目使用 XML Mapper。</p>
 */
@Mapper
public interface AlertStateMapper {

    /** 根据规则 ID 和服务器 ID 查询唯一状态行。 */
    AlertStateEntity selectByRuleAndServer(@Param("ruleId") Long ruleId, @Param("serverId") Long serverId);

    /** 插入新状态行，回写主键。 */
    int insertState(@Param("state") AlertStateEntity state);

    /** 更新状态为活跃，设置 alert_record_id 和 last_triggered_at，version + 1。 */
    int updateStateActive(@Param("id") Long id, @Param("alertRecordId") Long alertRecordId,
            @Param("lastTriggeredAt") LocalDateTime lastTriggeredAt, @Param("version") int version);

    /** 更新状态为已恢复，设置 active=0 和 resolved_at，version + 1。 */
    int updateStateResolved(@Param("id") Long id, @Param("resolvedAt") LocalDateTime resolvedAt,
            @Param("version") int version);
}
