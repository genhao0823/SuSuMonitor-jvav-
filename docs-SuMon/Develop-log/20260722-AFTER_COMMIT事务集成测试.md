# 开发日志: AFTER_COMMIT 事务集成测试

**日期**: 2026-07-22

**操作人**: opencode

## 改动内容

### 新增
- 在 Maven 测试作用域增加 H2 内存数据库，用于提供真实事务管理器，不影响生产依赖。
- 新增 `MonitorMetricsPublisherIntegrationTests`，以最小 Spring 上下文验证事务事件提交和回滚行为。
- Metrics Mapper、Server Mapper 和订阅注册表使用 Mockito 替身，不执行真实 SQL、不依赖本机 MySQL。

## 验证规则

- `MetricsService.report()` 所在事务成功提交后，Monitor WebSocket 发送一次 `metrics.update`。
- 事务显式标记回滚时，AFTER_COMMIT 监听器不得发送任何 WebSocket 消息。

## 涉及文件

- `server-java-SuMon/pom.xml`
- `server-java-SuMon/src/test/java/com/susumonitor/server/websocket/MonitorMetricsPublisherIntegrationTests.java`
- `docs-SuMon/Develop-log/20260722-AFTER_COMMIT事务集成测试.md`

## 验证结果

```text
mvn -Dtest=MonitorMetricsPublisherIntegrationTests test
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

验证层级：真实 Spring 事务同步 + H2 DataSourceTransactionManager + Mockito 数据访问替身。该测试直接覆盖事务提交/回滚事件边界，但不验证 MySQL SQL 方言。

## 备份留痕

- 修改前 `pom.xml` 已备份到 `C:\Backup\SuSuMonitor\execution-20260722\after-commit-20260722-025841\`。
- 备份文件已验证存在、非空且可读取。

## 当前进度

AFTER_COMMIT 正路径和回滚负路径均已自动化覆盖。下一模块将引入 UTC `Clock` Bean，使 Agent 10 秒认证超时和 90 秒离线边界可控测试。
