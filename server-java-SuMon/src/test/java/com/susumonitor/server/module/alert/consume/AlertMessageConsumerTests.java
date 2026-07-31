package com.susumonitor.server.module.alert.consume;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.susumonitor.server.module.alert.service.AlertEvaluationService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * AlertMessageConsumer 单元测试（全 mock，不依赖 Broker/DB）。
 *
 * <p>事务同步生命周期由测试驱动：在回调内注册同步，回调后触发
 * afterCommit——模拟真实事务管理器提交后调用，验证"业务成功才 ACK"语义。</p>
 */
@SuppressWarnings("unchecked")
class AlertMessageConsumerTests {

    private static final String EVENT_ID = "evt-20260731-0001";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AlertEvaluationService evaluationService;

    private ConsumeRecordMapper consumeRecordMapper;

    private TransactionTemplate transactionTemplate;

    private Channel channel;

    private AlertMessageConsumer consumer;

    @BeforeEach
    void setUp() {
        evaluationService = mock(AlertEvaluationService.class);
        consumeRecordMapper = mock(ConsumeRecordMapper.class);
        transactionTemplate = mock(TransactionTemplate.class);
        channel = mock(Channel.class);
        // 模拟 TransactionTemplate：执行回调并驱动事务同步生命周期（init -> commit）。
        doAnswer(invocation -> {
            java.util.function.Consumer<TransactionStatus> callback = invocation.getArgument(0);
            TransactionSynchronizationManager.initSynchronization();
            try {
                callback.accept(null);
                TransactionSynchronizationManager.getSynchronizations()
                        .forEach(TransactionSynchronization::afterCommit);
            } finally {
                TransactionSynchronizationManager.clear();
            }
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        consumer = new AlertMessageConsumer(objectMapper, evaluationService, consumeRecordMapper,
                transactionTemplate, Clock.fixed(Instant.parse("2026-07-31T08:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void idempotentHitAcksWithoutEvaluationOrInsert() throws Exception {
        when(consumeRecordMapper.existsConsumed(AlertMessageConsumer.CONSUMER_NAME, EVENT_ID)).thenReturn(true);

        consumer.onMessage(envelopeMessage(EVENT_ID, 1), channel, 42L);

        verify(channel).basicAck(42L, false);
        verify(evaluationService, never()).evaluate(any());
        verify(consumeRecordMapper, never()).insert(any());
    }

    @Test
    void firstConsumeEvaluatesInsertsRecordAndAcksAfterCommit() throws Exception {
        when(consumeRecordMapper.existsConsumed(AlertMessageConsumer.CONSUMER_NAME, EVENT_ID)).thenReturn(false);

        consumer.onMessage(envelopeMessage(EVENT_ID, 1), channel, 7L);

        verify(evaluationService).evaluate(any());
        verify(consumeRecordMapper).insert(org.mockito.ArgumentMatchers.argThat(record ->
                AlertMessageConsumer.CONSUMER_NAME.equals(record.getConsumer())
                        && EVENT_ID.equals(record.getEventId())
                        && ConsumeStatus.CONSUMED.ruleValue().equals(record.getStatus())
                        && record.getAttempts() == 0));
        verify(channel).basicAck(7L, false);
    }

    @Test
    void unparseableBodyRejectsWithoutRequeue() throws Exception {
        Message message = new Message("not-a-json{{".getBytes(StandardCharsets.UTF_8), new MessageProperties());

        assertThatThrownBy(() -> consumer.onMessage(message, channel, 1L))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(evaluationService);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void unsupportedSchemaVersionRejectsWithoutRequeue() throws Exception {
        Message message = new Message(envelopeJson(EVENT_ID, 2).getBytes(StandardCharsets.UTF_8),
                new MessageProperties());

        assertThatThrownBy(() -> consumer.onMessage(message, channel, 1L))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(evaluationService);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void evaluationExceptionPropagatesWithoutAck() throws Exception {
        when(consumeRecordMapper.existsConsumed(AlertMessageConsumer.CONSUMER_NAME, EVENT_ID)).thenReturn(false);
        doThrow(new RuntimeException("db unavailable")).when(evaluationService).evaluate(any());

        assertThatThrownBy(() -> consumer.onMessage(envelopeMessage(EVENT_ID, 1), channel, 3L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db unavailable");

        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    private Message envelopeMessage(String eventId, int schemaVersion) {
        return new Message(envelopeJson(eventId, schemaVersion).getBytes(StandardCharsets.UTF_8),
                new MessageProperties());
    }

    /** 构造符合冻结契约 §三 的信封 JSON（snake_case）。 */
    private String envelopeJson(String eventId, int schemaVersion) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("server_id", 1L);
        payload.put("message_id", "msg-0001");
        payload.put("collected_at", "2026-07-31T07:59:59Z");
        payload.put("cpu_percent", 80.5);
        payload.put("memory_percent", 70.1);
        payload.put("memory_used", 8192L);
        payload.put("memory_total", 16384L);
        payload.put("disk_percent", 55.2);
        payload.put("disk_used", 1024L);
        payload.put("disk_total", 2048L);
        payload.put("net_rx", 1000L);
        payload.put("net_tx", 2000L);
        payload.put("temperature", 65.3);
        payload.put("load_avg", 2.5);

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("event_id", eventId);
        envelope.put("event_type", "metrics.reported");
        envelope.put("schema_version", schemaVersion);
        envelope.put("occurred_at", "2026-07-31T08:00:00Z");
        envelope.put("producer", "metrics-service");
        envelope.put("payload", payload);
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
