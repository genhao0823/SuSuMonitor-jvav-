# 开发日志: Monitor 时间边界 Clock 可控化

**日期**: 2026-07-22

**操作人**: opencode

## 改动内容

### 修改
- `MonitorTicketService` 注入统一 UTC Clock，签发、消费和清理使用同一时间源。
- Ticket 在过期时间点恰好到达时立即失效，边界语义由“早于当前时间”收紧为“不得晚于当前时间”。
- `MonitorWebSocketHandler` 错误帧时间戳使用统一 Clock。
- `MonitorMetricsPublisher` 的 `metrics.update` 时间戳使用统一 Clock。
- AFTER_COMMIT 集成测试提供固定 Clock，消除广播时间的不确定性。

### 测试增强
- 移除 `MonitorTicketServiceTests` 中的 `Thread.sleep`。
- 验证 29.999 秒仍可消费、30 秒时立即拒绝。
- 验证定时清理移除过期未消费 Ticket。
- 验证两个并发消费者最多一个成功。

## 涉及文件

- `server-java-SuMon/src/main/java/com/susumonitor/server/websocket/MonitorTicketService.java`
- `server-java-SuMon/src/main/java/com/susumonitor/server/websocket/MonitorWebSocketHandler.java`
- `server-java-SuMon/src/main/java/com/susumonitor/server/websocket/MonitorMetricsPublisher.java`
- `server-java-SuMon/src/test/java/com/susumonitor/server/websocket/MonitorTicketServiceTests.java`
- `server-java-SuMon/src/test/java/com/susumonitor/server/websocket/MonitorMetricsPublisherIntegrationTests.java`

## 验证结果

```text
mvn -Dtest=MonitorTicketServiceTests,MonitorMetricsPublisherIntegrationTests test
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

mvn -DskipTests compile
BUILD SUCCESS

mvn -Dtest=!MetricsCleanupMySqlValidationTests test
Tests run: 162, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

验证层级：可推进 UTC Clock 单元测试 + H2 真实事务事件集成测试。未执行真实等待，未访问生产或开发数据库。

首次全量回归暴露 `MonitorTicketService` 存在生产、测试两个构造器时 Spring 无法自动选择注入入口。已按 Spring Framework 6.2 多构造器注入规则，在生产 Clock 构造器上显式添加 `@Autowired`；随后上下文定向回归 21/21、全量常规回归 162/162 均通过。

## 备份留痕

- 修改前 5 个 Monitor 源码/测试文件已备份到 `C:\Backup\SuSuMonitor\execution-20260722\monitor-clock-20260722-030624\`。
- 备份共 5 个文件，均已验证存在、非空且可读取。

## 当前进度

Monitor Ticket 30 秒边界、过期清理、单次并发消费、错误帧和广播 UTC 时间均已使用可控 Clock。下一步同步总开发记录并执行最终全量回归。
