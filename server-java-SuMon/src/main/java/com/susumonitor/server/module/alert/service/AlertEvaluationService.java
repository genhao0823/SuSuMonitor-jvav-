package com.susumonitor.server.module.alert.service;

import com.susumonitor.server.module.alert.entity.AlertRecordEntity;
import com.susumonitor.server.module.alert.entity.AlertRuleEntity;
import com.susumonitor.server.module.alert.entity.AlertStateEntity;
import com.susumonitor.server.module.alert.enums.AlertRecordStatus;
import com.susumonitor.server.module.alert.mapper.AlertRecordMapper;
import com.susumonitor.server.module.alert.mapper.AlertRuleMapper;
import com.susumonitor.server.module.alert.mapper.AlertStateMapper;
import com.susumonitor.server.module.alert.vo.AlertRecordVo;
import com.susumonitor.server.module.metrics.service.MetricsService.MetricsReportedEvent;
import com.susumonitor.server.module.metrics.vo.MetricsLatestVo;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 告警评估器，在 Metrics 事务提交后消费事件，评估规则并维护状态和记录。
 *
 * <p>使用 @TransactionalEventListener(AFTER_COMMIT) 确保只对已提交的
 * 指标数据进行评估。评估方法加 @Transactional 开启独立事务，告警评估
 * 失败不影响指标写入。评估事务提交后发布的 AlertTriggeredEvent 由
 * AlertPushPublisher 在 AFTER_COMMIT 推送 WebSocket。</p>
 *
 * <p>状态迁移通过 AlertStateMachine 纯逻辑判断，数据库操作通过 Mapper
 * 执行。乐观锁冲突时记录 warn 日志并跳过本轮，不重试。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEvaluationService {

    private final AlertRuleMapper ruleMapper;
    private final AlertStateMapper stateMapper;
    private final AlertRecordMapper recordMapper;
    private final AlertStateMachine stateMachine;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * 消费 Metrics 事件，评估该服务器匹配的所有启用规则。
     *
     * <p>AFTER_COMMIT 确保指标已落库。@Transactional 开启新事务，
     * 告警状态和记录写入独立于 Metrics 事务。</p>
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onMetricsReported(MetricsReportedEvent event) {
        MetricsLatestVo metrics = event.metrics();
        List<AlertRuleEntity> rules = ruleMapper.selectEnabledRulesForServer(metrics.getServerId());
        if (rules.isEmpty()) {
            return;
        }
        for (AlertRuleEntity rule : rules) {
            try {
                evaluateRule(rule, metrics);
            } catch (Exception exception) {
                log.warn("alert evaluation failed, ruleId={}, serverId={}, error={}",
                        rule.getId(), metrics.getServerId(), exception.getMessage());
            }
        }
    }

    /**
     * 评估单条规则，根据状态机决策执行对应操作。
     */
    private void evaluateRule(AlertRuleEntity rule, MetricsLatestVo metrics) {
        AlertStateEntity state = stateMapper.selectByRuleAndServer(rule.getId(), metrics.getServerId());
        AlertTransition transition = stateMachine.evaluate(rule, metrics, state);

        switch (transition) {
            case AlertTransition.Trigger t -> handleTrigger(rule, metrics.getServerId(), t.currentValue());
            case AlertTransition.ContinueBreached c -> handleContinue(c.state(), c.currentValue());
            case AlertTransition.Resolve r -> handleResolve(r.state());
            case AlertTransition.NoAction ignored -> {
            }
        }
    }

    /**
     * 首次越界：创建 unread record + active state，发布 AlertTriggeredEvent。
     */
    private void handleTrigger(AlertRuleEntity rule, Long serverId, BigDecimal currentValue) {
        LocalDateTime now = LocalDateTime.now(clock);
        // 创建告警记录。
        AlertRecordEntity record = new AlertRecordEntity();
        record.setRuleId(rule.getId());
        record.setServerId(serverId);
        record.setMetric(rule.getMetric());
        record.setCurrentValue(currentValue);
        record.setThresholdValue(rule.getThresholdValue());
        record.setLevel(rule.getLevel());
        record.setStatus(AlertRecordStatus.UNREAD.ruleValue());
        record.setMessage(buildMessage(rule.getMetric(), rule.getOperator(), currentValue, rule.getThresholdValue()));
        record.setTriggeredAt(now);
        recordMapper.insertRecord(record);

        // 创建活跃状态。
        AlertStateEntity state = new AlertStateEntity();
        state.setRuleId(rule.getId());
        state.setServerId(serverId);
        state.setActive(true);
        state.setAlertRecordId(record.getId());
        state.setFirstTriggeredAt(now);
        state.setLastTriggeredAt(now);
        state.setVersion(0);
        stateMapper.insertState(state);

        // 发布告警触发事件供 WS 推送。
        AlertRecordVo recordVo = toVo(record);
        eventPublisher.publishEvent(new AlertTriggeredEvent(serverId, recordVo));
    }

    /**
     * 持续越界：更新 last_triggered_at 和 version，不创建新 record。
     */
    private void handleContinue(AlertStateEntity state, BigDecimal currentValue) {
        int updated = stateMapper.updateStateActive(
                state.getId(), state.getAlertRecordId(),
                LocalDateTime.now(clock), state.getVersion());
        if (updated == 0) {
            log.warn("alert state optimistic lock conflict, stateId={}, version={}",
                    state.getId(), state.getVersion());
        }
    }

    /**
     * 恢复：标记 record resolved + state inactive，不发布事件。
     */
    private void handleResolve(AlertStateEntity state) {
        LocalDateTime now = LocalDateTime.now(clock);
        recordMapper.updateStatusToResolved(state.getAlertRecordId(), now);
        int updated = stateMapper.updateStateResolved(state.getId(), now, state.getVersion());
        if (updated == 0) {
            log.warn("alert state optimistic lock conflict during resolve, stateId={}, version={}",
                    state.getId(), state.getVersion());
        }
    }

    private String buildMessage(String metric, String operator, BigDecimal currentValue, BigDecimal threshold) {
        return metric + " " + operator + " " + threshold + " (current: " + currentValue + ")";
    }

    private AlertRecordVo toVo(AlertRecordEntity entity) {
        AlertRecordVo vo = new AlertRecordVo();
        vo.setId(entity.getId());
        vo.setRuleId(entity.getRuleId());
        vo.setServerId(entity.getServerId());
        vo.setMetric(entity.getMetric());
        vo.setCurrentValue(entity.getCurrentValue());
        vo.setThresholdValue(entity.getThresholdValue());
        vo.setLevel(entity.getLevel());
        vo.setStatus(entity.getStatus());
        vo.setMessage(entity.getMessage());
        vo.setTriggeredAt(AlertRecordVo.toOffset(entity.getTriggeredAt()));
        vo.setCreatedAt(AlertRecordVo.toOffset(entity.getCreatedAt()));
        return vo;
    }
}
