package com.susumonitor.server.module.metrics.outbox;

import com.susumonitor.server.module.metrics.entity.MetricsEntity;

/**
 * Outbox 业务契约：在指标入库事务内登记一条待可靠投递的冻结事件。
 */
public interface OutboxService {

    /**
     * 登记待发布事件（必须在调用方事务内执行，与指标写入同事务提交）。
     *
     * @param entity    已落库的指标实体
     * @param messageId Agent 上报消息 UUID
     */
    void enqueue(MetricsEntity entity, String messageId);
}
