package com.susumonitor.server.module.metrics.outbox;

import com.susumonitor.server.module.metrics.entity.MetricsEntity;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Outbox 登记实现：生成事件 ID、构建冻结信封并插入待发布行。
 *
 * <p>本类不开启独立事务，由调用方（Metrics 写入事务）保证
 * 指标与 outbox 同事务提交或回滚。</p>
 */
@Service
public class OutboxServiceImpl implements OutboxService {

    private final OutboxMapper outboxMapper;

    private final OutboxEnvelopeFactory envelopeFactory;

    private final Clock clock;

    /** 注入 Outbox 数据访问、信封工厂与应用时钟。 */
    public OutboxServiceImpl(OutboxMapper outboxMapper, OutboxEnvelopeFactory envelopeFactory, Clock clock) {
        this.outboxMapper = outboxMapper;
        this.envelopeFactory = envelopeFactory;
        this.clock = clock;
    }

    /** {@inheritDoc} */
    @Override
    public void enqueue(MetricsEntity entity, String messageId) {
        String eventId = UUID.randomUUID().toString();
        OutboxEntity outbox = new OutboxEntity();
        outbox.setEventId(eventId);
        outbox.setEventType(OutboxEnvelopeFactory.EVENT_TYPE);
        outbox.setPayload(envelopeFactory.build(entity, messageId, eventId));
        outbox.setStatus(OutboxStatus.PENDING.ruleValue());
        outbox.setAttempts(0);
        outbox.setNextAttemptAt(LocalDateTime.now(clock));
        outboxMapper.insert(outbox);
    }
}
