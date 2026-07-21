# 开发日志: WebSocket 监控通道中危缺陷修复与健壮性增强

**日期**: 2026-07-21
**操作人**: opencode

## 改动内容

### 修复
- 修复 `MonitorTicketService` 过期未消费 ticket 在内存无界累积的泄漏问题。
- 修复 `AgentWebSocketHandler` 10 秒未认证扫描与慢 DB 认证竞态，可能导致已关闭 session 被注册进 connectionRegistry。
- 修复 `AgentWebSocketHandler` 认证中 `heartbeat` 抛 `IllegalStateException` 被误报为 "invalid agent message"。
- 修复 `MonitorHandshakeInterceptor` `BusinessException` 被静默吞掉，无日志、无 HTTP 状态码。
- 修复 `MonitorWebSocketHandler` JSON 解析异常未捕获导致一条坏消息即断开 Monitor 连接。

### 增强
- `MonitorTicketService` 新增 `@Scheduled` 每分钟清理过期未消费的 ticket entry。
- `AgentWebSocketSession` 增加 `volatile authenticating` 标记，认证中状态跳过超时扫描。
- `AgentWebSocketHandler` 注册前校验 `socketSession().isOpen()`，避免注册已关闭连接。
- `MonitorHandshakeInterceptor` 拒绝时 `log.warn` 记录原因（不含 ticket 值）并返回 401。
- `MonitorWebSocketHandler` 坏 JSON 回送错误帧并保留连接；清理 `serverNode == null` 死代码。
- `MonitorTicketService` 新增包私有构造器注入短 TTL，便于测试过期清理。

### 测试
- `MonitorTicketServiceTests` 增加 `expiredTicketShouldBeRejectedAndPurged`，验证过期消费被拒且清理任务移除 entry。

## 涉及文件

- server-java-SuMon/src/main/java/com/susumonitor/server/websocket/AgentWebSocketSession.java
- server-java-SuMon/src/main/java/com/susumonitor/server/websocket/AgentWebSocketHandler.java
- server-java-SuMon/src/main/java/com/susumonitor/server/websocket/MonitorTicketService.java
- server-java-SuMon/src/main/java/com/susumonitor/server/websocket/MonitorHandshakeInterceptor.java
- server-java-SuMon/src/main/java/com/susumonitor/server/websocket/MonitorWebSocketHandler.java
- server-java-SuMon/src/test/java/com/susumonitor/server/websocket/MonitorTicketServiceTests.java

## 验证结果

| 验证对象 | 命令 | 实际结果 |
| --- | --- | --- |
| 后端编译 | `mvn -DskipTests compile` | BUILD SUCCESS |
| 定向测试 | `mvn -Dtest=MonitorTicketServiceTests test` | 2/2 通过 |
| 全量回归 | `mvn -Dtest=!MetricsCleanupMySqlValidationTests test` | 155/155 通过，0 失败 0 错误 |
| 差异检查 | `git diff --check` | 通过 |

## 备份留痕

- 修改前已备份 5 个源文件到 `C:\Backup\SuSuMonitor\execution-20260721\bugfix-20260721-175855\`，共 5 文件非空可读。

## 当前进度

阶段 0（缺陷修复）已完成。后续阶段：隔离库 + 18081 实例、Agent Token REST API 真实验收、`/ws/agent` 客户端真实验收、`/ws/monitor` 广播真实验收、文档同步。

## 备注

- `MetricsCleanupMySqlValidationTests` 仍因本机未提供 `susumonitor` 数据库密码在 Spring 容器初始化阶段失败，非代码断言失败，已从回归中排除。
- 隔离数据库 `susumonitor_metrics_cleanup_validation_20260721` 继续保留，未删除。
