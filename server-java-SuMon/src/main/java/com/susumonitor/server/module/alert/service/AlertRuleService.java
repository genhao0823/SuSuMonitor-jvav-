package com.susumonitor.server.module.alert.service;

import com.susumonitor.server.module.alert.dto.CreateAlertRuleRequest;
import com.susumonitor.server.module.alert.dto.UpdateAlertRuleRequest;
import com.susumonitor.server.module.alert.vo.AlertRuleVo;
import java.util.List;

/**
 * 定义告警规则维护的业务契约。
 */
public interface AlertRuleService {

    /** 创建告警规则。 */
    AlertRuleVo createRule(CreateAlertRuleRequest request, Long createdBy);

    /** 更新告警规则。 */
    AlertRuleVo updateRule(Long ruleId, UpdateAlertRuleRequest request);

    /** 软删除告警规则。 */
    void deleteRule(Long ruleId);

    /** 查询启用的告警规则。 */
    List<AlertRuleVo> listRules();
}
