# Java Service 接口化

**日期**: 2026-07-26
**状态**: 已实现并完成隔离 MySQL、真实 Java/Go Agent 验证

## 改动摘要

- 将 14 个生产 `*Service` 拆分为同包业务接口和单一 `*ServiceImpl` 实现。
- Controller、WebSocket Handler、Scheduler、Security 组件继续以原 `*Service` 名称注入，现均指向接口类型。
- Spring `@Service`、`@Transactional`、`@Scheduled` 和 `@TransactionalEventListener` 留在实现类，保留原有代理、事务和事件触发语义。
- 将 `MetricsReportedEvent`、`MetricsCleanupService.CleanupResult`、JWT 的 `IssuedToken` 与 `ParsedToken` 迁入接口，避免调用方依赖具体实现类。
- 单元测试直接构造实现类；Controller 与 Spring 集成测试维持接口 mock 或接口注入。

## 涉及文件

- `server-java-SuMon/src/main/java/com/susumonitor/server/module/**/service/*Service.java`
- `server-java-SuMon/src/main/java/com/susumonitor/server/module/**/service/*ServiceImpl.java`
- `server-java-SuMon/src/main/java/com/susumonitor/server/websocket/*Service.java`
- `server-java-SuMon/src/main/java/com/susumonitor/server/websocket/*ServiceImpl.java`
- `server-java-SuMon/src/main/java/com/susumonitor/server/security/JwtTokenService.java`
- `server-java-SuMon/src/main/java/com/susumonitor/server/security/JwtTokenServiceImpl.java`
- 关联 Service 单元测试及指标事务事件集成测试。

## 验证记录

- 已通过：`mvn test`。
- 结果：283 tests，0 failures，0 errors，0 skipped。
- 已通过：`go test ./...`，覆盖 Agent Go 全部包。
- 已通过：`node --check verify-go-agent-reconnect.mjs`、`node --check verify-go-agent-recovery.mjs`、`node --check verify-p2-agent-limits.mjs`。
- 已通过：在 `web-vue-SuMon` 执行 `npm run openapi:check`，5/5 OpenAPI 契约与 Controller 路径通过。
- 已通过：在短生命周期 PowerShell 中对 `127.0.0.1:3306/susumonitor_metrics_validation` 执行 `mvn -Pmysql-validation verify`。Flyway 成功校验 V1-V11，11 个 MySQL 集成测试通过，0 failures，0 errors，0 skipped。
- 已通过：`api-test/run-p2-agent-e2e.ps1`。Go Agent 重连退避 6 项、服务恢复与 Token 轮换 6 项、P2 连接/消息限流 4 项均通过，且未输出 Token 值。
- 运行后确认：临时 Java 服务端口 `18081` 与 `18082` 均已停止；数据库密码已从验证子进程清除。
