# 开发日志: Agent 时间边界 Clock 可控化

**日期**: 2026-07-22

**操作人**: opencode

## 改动内容

### 新增
- 新增 `ClockConfig`，生产环境统一提供 `Clock.systemUTC()`。
- 新增 `AgentWebSocketHandlerTests`，覆盖 10 秒未认证超时和认证中慢连接保护。
- 新增 `AgentHeartbeatServiceTests`，覆盖 89 秒不离线、91 秒离线的边界。

### 修改
- `AgentWebSocketSession` 使用注入 Clock 记录连接时间。
- `AgentWebSocketHandler` 使用 Clock 生成认证时间、超时截止时间和协议 UTC 时间戳。
- `AgentHeartbeatService` 使用 Clock 生成心跳时间和 90 秒离线截止时间。

## 涉及文件

- `server-java-SuMon/src/main/java/com/susumonitor/server/config/ClockConfig.java`
- `server-java-SuMon/src/main/java/com/susumonitor/server/websocket/AgentWebSocketSession.java`
- `server-java-SuMon/src/main/java/com/susumonitor/server/websocket/AgentWebSocketHandler.java`
- `server-java-SuMon/src/main/java/com/susumonitor/server/websocket/AgentHeartbeatService.java`
- `server-java-SuMon/src/test/java/com/susumonitor/server/websocket/AgentWebSocketHandlerTests.java`
- `server-java-SuMon/src/test/java/com/susumonitor/server/websocket/AgentHeartbeatServiceTests.java`

## 验证结果

```text
mvn -Dtest=AgentWebSocketHandlerTests,AgentHeartbeatServiceTests test
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

mvn -DskipTests compile
BUILD SUCCESS
```

验证层级：Mockito 单元测试 + 可推进 UTC Clock。测试不使用 `Thread.sleep`，认证中保护测试通过并发阻塞真实鉴权路径复现慢 DB 场景。

## 备份留痕

- 修改前 3 个 Agent 源文件已备份到 `C:\Backup\SuSuMonitor\execution-20260722\agent-clock-20260722-030200\`。
- 备份共 3 个文件，均已验证存在、非空且可读取。

## 当前进度

Agent 10 秒认证超时和 90 秒离线边界已具备无等待自动化测试。下一模块将改造 Monitor Ticket、错误帧和指标广播时间源，并移除 Ticket 测试中的 sleep。
