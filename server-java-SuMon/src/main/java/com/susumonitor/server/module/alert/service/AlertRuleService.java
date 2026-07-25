package com.susumonitor.server.module.alert.service;

import com.susumonitor.server.common.BusinessException;
import com.susumonitor.server.common.ErrorCode;
import com.susumonitor.server.module.alert.dto.CreateAlertRuleRequest;
import com.susumonitor.server.module.alert.dto.UpdateAlertRuleRequest;
import com.susumonitor.server.module.alert.entity.AlertRuleEntity;
import com.susumonitor.server.module.alert.enums.AlertLevel;
import com.susumonitor.server.module.alert.enums.AlertMetric;
import com.susumonitor.server.module.alert.enums.AlertOperator;
import com.susumonitor.server.module.alert.mapper.AlertRuleMapper;
import com.susumonitor.server.module.alert.vo.AlertRuleVo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 告警规则 CRUD 服务，负责规则创建、查询、更新和软删除。
 *
 * <p>创建时校验 metric/operator/level 字符串是否为合法枚举值。
 * 删除采用软删除，标记 deleted=1，不物理删除。</p>
 */
@Service
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleMapper ruleMapper;
    private final Clock clock;

    /** 创建告警规则，校验 metric/operator/level 合法性。 */
    @Transactional
    public AlertRuleVo createRule(CreateAlertRuleRequest request, Long createdBy) {
        validateCreateRequest(request);
        AlertRuleEntity entity = new AlertRuleEntity();
        entity.setServerId(request.getServerId());
        entity.setMetric(request.getMetric());
        entity.setOperator(request.getOperator());
        entity.setThresholdValue(request.getThresholdValue());
        entity.setLevel(request.getLevel());
        entity.setEnabled(true);
        entity.setDeleted(false);
        entity.setCreatedBy(createdBy);
        ruleMapper.insertRule(entity);
        return toVo(ruleMapper.selectActiveRuleById(entity.getId()));
    }

    /** 更新规则阈值、等级和启用状态，不允许修改 metric/operator/serverId。 */
    @Transactional
    public AlertRuleVo updateRule(Long ruleId, UpdateAlertRuleRequest request) {
        AlertRuleEntity entity = ruleMapper.selectActiveRuleById(ruleId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        validateLevel(request.getLevel());
        ruleMapper.updateRule(ruleId, request.getThresholdValue(), request.getLevel(), request.getEnabled());
        return toVo(ruleMapper.selectActiveRuleById(ruleId));
    }

    /** 软删除规则，标记 deleted=1。 */
    @Transactional
    public void deleteRule(Long ruleId) {
        AlertRuleEntity entity = ruleMapper.selectActiveRuleById(ruleId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        ruleMapper.softDeleteRule(ruleId, LocalDateTime.now(clock));
    }

    /** 查询所有未删除规则。 */
    @Transactional(readOnly = true)
    public List<AlertRuleVo> listRules() {
        return ruleMapper.selectActiveRules().stream().map(this::toVo).toList();
    }

    private void validateCreateRequest(CreateAlertRuleRequest request) {
        if (AlertMetric.fromValue(request.getMetric()) == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }
        if (AlertOperator.fromValue(request.getOperator()) == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }
        validateLevel(request.getLevel());
    }

    private void validateLevel(String level) {
        if (AlertLevel.fromValue(level) == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }
    }

    private AlertRuleVo toVo(AlertRuleEntity entity) {
        if (entity == null) {
            return null;
        }
        AlertRuleVo vo = new AlertRuleVo();
        vo.setId(entity.getId());
        vo.setServerId(entity.getServerId());
        vo.setMetric(entity.getMetric());
        vo.setOperator(entity.getOperator());
        vo.setThresholdValue(entity.getThresholdValue());
        vo.setLevel(entity.getLevel());
        vo.setEnabled(entity.getEnabled());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedAt(AlertRuleVo.toOffset(entity.getCreatedAt()));
        vo.setUpdatedAt(AlertRuleVo.toOffset(entity.getUpdatedAt()));
        return vo;
    }
}
