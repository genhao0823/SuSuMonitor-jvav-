# Agent 与 Monitor 实时链路执行记录

## 已实现

- Monitor Ticket：`POST /api/ws/monitor-ticket`，Ticket 使用 32 字节随机值，30 秒有效且原子单次消费。
- `/ws/monitor` 握手拦截器从 URL 读取短时 Ticket，不读取长期 JWT 或 Agent Token。
- Monitor 连接按用户、会话和 `server_id` 建立单 JVM 订阅索引，订阅前校验服务器存在和用户审核状态。
- Metrics 事务提交后通过 `@TransactionalEventListener(AFTER_COMMIT)` 广播 `metrics.update`。
- Agent 首帧认证超时 10 秒；认证成功更新在线状态和心跳时间；90 秒无心跳进入离线扫描。
- Agent、Monitor、用户和 Metrics 时间转换统一采用 UTC。
- 前端先通过 HTTP 获取 Monitor Ticket，再使用 Ticket 建立 `/ws/monitor` 连接。
- OpenAPI、HTTP 样例、WebSocket 协议和项目需求地址已同步。
- Metrics AFTER_COMMIT 已补充 H2 真实事务管理器集成测试，覆盖提交广播和回滚不广播。
- Agent 和 Monitor 时间边界已统一注入 UTC `Clock`，10 秒认证、90 秒离线和 30 秒 Ticket 均可无等待测试。

## 代码验证

| 验证对象 | 命令 | 实际结果 |
| --- | --- | --- |
| 后端编译 | `mvn -DskipTests compile` | 通过 |
| 后端定向测试 | `mvn -Dtest=MonitorMetricsPublisherIntegrationTests,AgentWebSocketHandlerTests,AgentHeartbeatServiceTests,MonitorTicketServiceTests test` | 9/9 通过 |
| 后端常规回归 | `mvn -Dtest=!MetricsCleanupMySqlValidationTests test` | 162/162 通过 |
| 前端 lint/typecheck/build | `npm run lint; npm run typecheck; npm run build` | 三项通过 |
| OpenAPI JSON | Node JSON.parse 与路径检查 | 通过，6 个新增路径存在 |
| 差异检查 | `git diff --check` | 通过 |

## 真实链路验收

| 验收对象 | 技术栈 | 实际结果 |
| --- | --- | --- |
| Agent Token REST | PowerShell 5.1 + 真实 HTTP + MySQL 8.4 | 19 项通过，含注册、轮换、撤销、哈希和错误码 |
| `/ws/agent` | Node.js + `ws` + 真实 HTTP + MySQL 8.4 | 14 项通过，含鉴权、心跳、Metrics、历史查询和旧 Token 失效 |
| `/ws/monitor` | Node.js + `ws` + 真实 Agent/Monitor 双连接 + MySQL 8.4 | 16 项通过，含 Ticket、订阅、实时广播、取消订阅和 Ticket 二次使用拒绝 |
| 数据库迁移 | Flyway + 隔离 MySQL | V1-V9 全部 `success=1` |

隔离实例使用 `http://localhost:18081`，隔离数据库使用 `susumonitor_agent_ws_validation_20260721`。所有真实凭据和 Token 只通过环境变量或内存传递，未写入文档或版本控制文件。

## 未验证与边界

- 排除需要显式隔离 MySQL 凭据的 `MetricsCleanupMySqlValidationTests` 后，常规回归共 162 个测试，0 失败、0 错误。该 MySQL 清理测试已有历史隔离库验收记录，本轮未对保留库重复执行破坏性清理。
- `metrics.update` 已通过 H2 `DataSourceTransactionManager` 集成测试直接验证：提交发送一次，显式回滚发送零次；该测试不覆盖 MySQL SQL 方言。
- 第一版只支持单 JVM，未验证多实例连接、Ticket 和订阅状态共享。
- 隔离数据库继续保留，未执行删除；删除需用户确认并先完成可读取的备份。

## 下一步计划

### 1. AFTER_COMMIT 负路径集成测试（已完成）

- 技术栈：Spring Test、JUnit 5、H2、`DataSourceTransactionManager`、Mockito。
- 实际结果：提交和回滚 2/2 通过；H2 只驱动真实事务同步，Mapper 不执行 SQL，因此无数据库测试数据残留。

### 2. Agent 时间边界可控化（已完成）

- 技术栈：Java `Clock`、Spring Bean 注入、JUnit 5、Mockito。
- 代码位置：`AgentWebSocketSession`、`AgentWebSocketHandler`、`AgentHeartbeatService`。
- 实现细节：以 `Clock` 替代散落的 `Instant.now()` 和 `LocalDateTime.now(UTC)`；测试固定推进 10 秒认证超时和 90 秒离线阈值，不执行真实等待。
- 实际结果：9 秒保留、11 秒关闭；认证中慢连接不被误杀；89 秒保持在线、91 秒标记离线并关闭连接。

### 3. Monitor Ticket 时间可控化（已完成）

- 技术栈：Java `Clock`、Spring 定时任务、JUnit 5。
- 代码位置：`MonitorTicketService`、`MonitorTicketServiceTests`。
- 实现细节：默认注入 UTC system Clock；测试注入 fixed/mutable Clock，直接推进到 30 秒前后，移除当前 `Thread.sleep`。
- 实际结果：29.999 秒可消费、到达 30 秒时拒绝、清理任务移除未消费过期 Ticket、并发消费最多成功一次；测试已移除 `Thread.sleep`。

### 4. 多 JVM 演进评估

- 技术栈候选：Redis TTL、Redis Pub/Sub 或 Spring Data Redis、实例路由标识。
- 设计范围：Ticket 原子消费、Agent 连接归属、Monitor 订阅索引、跨实例 `metrics.update` 广播。
- 前置条件：只有确认进入多实例部署后才实现；第一版继续保持单 JVM，不提前引入分布式复杂度。

### 5. 隔离资源收口

- 保留：`susumonitor_agent_ws_validation_20260721`、`susumonitor_metrics_cleanup_validation_20260721`。
- 删除前流程：展示目标和影响范围 → `mysqldump` → 验证备份存在、非空、可读取 → 用户最终确认 → 删除 → 验证数据库不存在 → 更新开发日志。
