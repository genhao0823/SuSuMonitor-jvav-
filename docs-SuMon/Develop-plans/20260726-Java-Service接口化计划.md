# 开发计划: Java Service 接口化

**日期**: 2026-07-26
**状态**: 已完成

## 目标

将 Java 后端所有 `*Service` 拆分为业务接口和单一 `*ServiceImpl` 实现，确保上层组件只依赖接口，同时不改变 REST、WebSocket、数据库或事件契约。

## 任务清单

- [x] 盘点 14 个生产 `*Service` 的调用方、事务和事件依赖。
- [x] 完成认证与服务器领域 Service 接口化。
- [x] 完成指标与 WebSocket Service 接口化。
- [x] 完成告警与 JWT Service 接口化。
- [x] 执行单元测试、MySQL 验证和 Agent 端到端回归。

## 预期时间

预计 1 个开发周期完成，按领域分批验证。

## 依赖关系

- `@Transactional`、`@Scheduled` 和 `@TransactionalEventListener` 保留在实现类方法，维持 Spring AOP 生效条件。
- `MetricsReportedEvent` 和 JWT 结果 record 保持在接口中，避免调用方依赖实现类。
- 现有未提交的 P2-1、V11 和 Agent-Go 改动不在本计划中回滚或修改。

## 备注

本次为内部依赖重构。若不改变公开 API 或协议，无需更新 OpenAPI 或 WebSocket 协议文档。
