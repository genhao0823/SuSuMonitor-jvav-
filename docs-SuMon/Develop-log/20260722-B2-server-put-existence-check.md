# B2 `PUT /api/servers/{id}` 资源存在性校验修复

**执行日期**：2026-07-22
**执行批次**：B2
**模块状态**：代码已实现；自动化测试通过；真实 HTTP 待验证

## 一、问题和根因

修复前，`@Valid @RequestBody UpdateServerRequest` 会在 Controller 方法执行前触发 DTO 校验。不存在 ID 且 body 不完整时会先返回 `40002`，而不是 `40400`。

## 二、修改前备份

已备份实际修改的 5 个文件：

```text
C:\Backup\SuSuMonitor\execution-20260723\b2-put-existence-20260722-170032\
```

5 个备份文件均存在且非空，未删除任何文件。

## 三、技术实现

技术栈：Java 21、Spring Boot 3.4.7、Spring MVC、Jakarta Bean Validation `Validator`、MyBatis、JUnit 5、Mockito、MockMvc。

实现细节：

1. `ServerService` 新增 `existsActive(Long serverId)`。
2. 复用 `selectActiveServerById`，只读取有效服务器公开字段，不读取 SSH 凭据。
3. Controller 更新入口先检查 active 服务器是否存在。
4. 不存在时返回 `40400`。
5. 存在后才执行 `UpdateServerRequest` 校验。
6. Service 原有业务校验、凭据状态机、事务和二次查询保持不变。
7. 未修改全局异常处理器、数据库迁移、OpenAPI、前端和 SSH 业务代码。

## 四、验证结果

- 修改前定向测试：38/38 通过。
- 修改后定向测试：`mvn -Dtest=ServerControllerTests,ServerServiceTests test`，42/42 通过。
- Java 回归：`mvn -Dtest=!MetricsCleanupMySqlValidationTests test`，166/166 通过。
- 编译：`mvn -DskipTests compile`，通过。
- OpenAPI：`npm run openapi:check`，3/3 通过。
- 健康检查：`/api/health`、`/api/ready` 均 HTTP 200。
- `git diff --check`：通过。

自动化测试覆盖：

- 不存在 ID + 不完整 body 返回 `40400`。
- 有效 ID + 不完整 body 返回 `40002`。
- 有效 ID + 合法 body 仍调用更新 Service。
- `existsActive` 对有效和不存在服务器返回正确结果。
- 不存在目标不调用更新 Service。

## 五、真实 HTTP 边界

本轮未执行需要管理员 JWT 的真实 PUT 请求，当前没有保存管理员 JWT。

待验证请求：

```http
PUT /api/servers/99999
Content-Type: application/json

{"description":"b2-probe"}
```

期望 HTTP 404、业务码 `40400`；该请求不修改数据库。

真实 HTTP 完成前，Bug 索引保持“代码已修复，真实 HTTP 待验证”。

## 六、敏感信息与回滚

- 数据库密码、JWT、Agent Token、SSH 凭据未写入代码、日志或 Git。
- 未执行数据库写入、删除或迁移。
- 回滚使用 B2 备份目录中的对应文件，恢复后重新执行定向测试和 `git diff --check`。

## 七、后续动作

1. 使用隔离服务和安全注入的管理员 JWT 完成真实 HTTP 无写入验证。
2. 真实 HTTP 通过后更新 Bug 验收勾选项。
3. 进入 B2-R，复核排序、SSH 错误码和软删除三个遗留问题。
