package com.susumonitor.server.module.alert.controller;

import com.susumonitor.server.common.ApiResponse;
import com.susumonitor.server.module.alert.dto.CreateAlertRuleRequest;
import com.susumonitor.server.module.alert.dto.UpdateAlertRuleRequest;
import com.susumonitor.server.module.alert.service.AlertRuleService;
import com.susumonitor.server.module.alert.vo.AlertRuleVo;
import com.susumonitor.server.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 告警规则 REST API，提供规则创建、查询、更新和删除。
 *
 * <p>创建、更新和删除需要 admin 角色，查询需要已认证用户。</p>
 */
// 将当前类注册为 REST Controller，并将返回值写入 HTTP 响应体。
@RestController
// 为告警规则接口统一增加 /api/alerts/rules 路径前缀。
@RequestMapping("/api/alerts/rules")
// 启用方法参数约束，使非正数 ID 返回参数错误。
@Validated
// 自动生成包含 final 字段的构造方法，用于构造方法依赖注入。
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    /** 创建告警规则，仅 admin。 */
    @PostMapping
    public ApiResponse<AlertRuleVo> createRule(
            // 触发 CreateAlertRuleRequest 的 Bean Validation 校验。
            @Valid
            // 将 HTTP JSON 请求体反序列化为 CreateAlertRuleRequest。
            @RequestBody CreateAlertRuleRequest request,
            // 从 Spring SecurityContext 注入当前认证用户。
            @AuthenticationPrincipal AuthenticatedUser operator) {
        return ApiResponse.success(alertRuleService.createRule(request, operator.id()));
    }

    /** 查询所有未删除规则，已认证用户可查。 */
    @GetMapping
    public ApiResponse<List<AlertRuleVo>> listRules() {
        return ApiResponse.success(alertRuleService.listRules());
    }

    /** 更新规则，仅 admin。 */
    @PutMapping("/{id}")
    public ApiResponse<AlertRuleVo> updateRule(
            // 将路径参数绑定为规则 ID。
            @PathVariable("id")
            // 限制规则 ID 必须大于 0。
            @Positive Long ruleId,
            @Valid @RequestBody UpdateAlertRuleRequest request) {
        return ApiResponse.success(alertRuleService.updateRule(ruleId, request));
    }

    /** 软删除规则，仅 admin。 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRule(
            // 将路径参数绑定为规则 ID。
            @PathVariable("id")
            // 限制规则 ID 必须大于 0。
            @Positive Long ruleId) {
        alertRuleService.deleteRule(ruleId);
        return ApiResponse.success(null);
    }
}
