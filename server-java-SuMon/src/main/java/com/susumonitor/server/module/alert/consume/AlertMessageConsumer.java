package com.susumonitor.server.module.alert.consume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.susumonitor.server.module.alert.service.AlertEvaluationService;
import com.susumonitor.server.module.metrics.outbox.OutboxEnvelopeFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 告警评估消息消费者（MVP-11）：幂等消费 metrics.reported.v1。
 *
 * <p>ACK 语义（对照冻结契约 rabbitmq-topology-v1.md §四）：仅在业务事务成功
 * （或已确认该 event_id 幂等完成）后 ACK；评估结果与消费幂等记录同事务提交。</p>
 *
 * <p>错误分类：JSON 解析失败 / schema_version 不支持 / event_type 不符属
 * 不可重试数据错误 → {@link AmqpRejectAndDontRequeueException} 直接进 DLQ；
 * 其余异常抛出，由容器级重试（有限次数）处理后 reject 进 DLQ。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "susumonitor.rabbitmq.enabled", havingValue = "true")
public class AlertMessageConsumer {

    static final String CONSUMER_NAME = "alert-evaluator";

    static final String QUEUE = "susumonitor.alert.metrics";

    private final ObjectMapper objectMapper;

    private final AlertEvaluationService evaluationService;

    private final ConsumeRecordMapper consumeRecordMapper;

    private final TransactionTemplate transactionTemplate;

    private final Clock clock;

    /** 注入反序列化器、评估服务、幂等记录数据访问与事务模板。 */
    public AlertMessageConsumer(ObjectMapper objectMapper, AlertEvaluationService evaluationService,
            ConsumeRecordMapper consumeRecordMapper, TransactionTemplate transactionTemplate, Clock clock) {
        this.objectMapper = objectMapper;
        this.evaluationService = evaluationService;
        this.consumeRecordMapper = consumeRecordMapper;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * 消费一条 metrics.reported.v1 消息。
     *
     * @param message     原始消息（UTF-8 JSON 信封）
     * @param channel     AMQP 通道（MANUAL ACK 模式）
     * @param deliveryTag 投递标记
     */
    @RabbitListener(queues = QUEUE, ackMode = "MANUAL")
    public void onMessage(Message message, com.rabbitmq.client.Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        MetricsReportedMessage envelope = parseEnvelope(message);
        if (consumeRecordMapper.existsConsumed(CONSUMER_NAME, envelope.eventId())) {
            // 幂等命中：已成功消费过（如 ACK 丢失后的重新投递），不产生第二次业务效果。
            log.debug("consume idempotent hit, eventId={}", envelope.eventId());
            acknowledge(channel, deliveryTag);
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            evaluationService.evaluate(envelope.payload().toMetricsLatestVo());
            insertConsumeRecord(envelope);
            // 业务事务提交后才 ACK（契约：仅在业务事务成功后 ACK）。
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    acknowledge(channel, deliveryTag);
                }
            });
        });
    }

    /**
     * 反序列化并校验信封；不可重试数据错误直接拒绝（进 DLQ）。
     */
    private MetricsReportedMessage parseEnvelope(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        MetricsReportedMessage envelope;
        try {
            envelope = objectMapper.readValue(body, MetricsReportedMessage.class);
        } catch (JsonProcessingException exception) {
            log.warn("consume rejected: unparseable envelope");
            throw new AmqpRejectAndDontRequeueException("unparseable envelope", exception);
        }
        if (envelope.schemaVersion() != OutboxEnvelopeFactory.SCHEMA_VERSION
                || !OutboxEnvelopeFactory.EVENT_TYPE.equals(envelope.eventType())) {
            log.warn("consume rejected: unsupported schemaVersion={} or eventType={}",
                    envelope.schemaVersion(), envelope.eventType());
            throw new AmqpRejectAndDontRequeueException("unsupported schema version or event type");
        }
        return envelope;
    }

    private void insertConsumeRecord(MetricsReportedMessage envelope) {
        ConsumeRecordEntity record = new ConsumeRecordEntity();
        record.setConsumer(CONSUMER_NAME);
        record.setEventId(envelope.eventId());
        record.setStatus(ConsumeStatus.CONSUMED.ruleValue());
        record.setAttempts(0);
        record.setConsumedAt(LocalDateTime.now(clock));
        consumeRecordMapper.insert(record);
    }

    /**
     * 执行 ACK；失败仅记录（消息将重新投递，由幂等记录兜底，不产生第二次业务效果）。
     */
    void acknowledge(com.rabbitmq.client.Channel channel, long deliveryTag) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException exception) {
            log.warn("consume ack failed, deliveryTag={}, will be redelivered", deliveryTag, exception);
        }
    }
}
