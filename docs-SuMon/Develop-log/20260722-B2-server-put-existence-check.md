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

## 五、Apifox 运行记录

Apifox CLI 已登录并确认项目 `8585366`、本地环境 `47408671` 和更新接口 `489125515` 存在。项目现有用例未配置有效的管理员 Token 全局变量。

执行：

```powershell
apifox test-case run 395661082 --project 8585366 --environment 47408671
```

该用例实际调用本地服务，但由于 `{{adminToken}}` 未注入有效 JWT，服务返回：

```text
HTTP 401
business code 40100
```

Apifox 已完成真实请求，但该结果只能证明未授权请求被安全链拒绝，不能证明 B2 的不存在服务器 PUT 返回 `40400`。没有把密码、JWT 或 Token 写入 Apifox 项目、仓库或日志。

Apifox 记录备份：

```text
C:\Backup\SuSuMonitor\execution-20260723\b2-apifox-20260722-173346\
```

自动化测试覆盖：

- 不存在 ID + 不完整 body 返回 `40400`。
- 有效 ID + 不完整 body 返回 `40002`。
- 有效 ID + 合法 body 仍调用更新 Service。
- `existsActive` 对有效和不存在服务器返回正确结果。
- 不存在目标不调用更新 Service。

## 六、真实 HTTP 边界

本轮未执行需要管理员 JWT 的真实 PUT 请求，当前没有保存管理员 JWT。

待验证请求：

```http
PUT /api/servers/99999
Content-Type: application/json

{"description":"b2-probe"}
```

期望 HTTP 404、业务码 `40400`；该请求不修改数据库。

真实 HTTP 完成前，Bug 索引保持“代码已修复，真实 HTTP 待验证”。

## 七、敏感信息与回滚

- 数据库密码、JWT、Agent Token、SSH 凭据未写入代码、日志或 Git。
- 未执行数据库写入、删除或迁移。
- 回滚使用 B2 备份目录中的对应文件，恢复后重新执行定向测试和 `git diff --check`。

## 八、后续动作

1. 使用隔离服务和安全注入的管理员 JWT 完成真实 HTTP 无写入验证。
2. 真实 HTTP 通过后更新 Bug 验收勾选项。
3. 进入 B2-R，复核排序、SSH 错误码和软删除三个遗留问题。

## 九、Apifox CLI 接口测试记录

### 隔离测试管理员

目标隔离库原本没有用户，且 `auth_bootstrap_state.admin_initialized=0`。通过最新后端的 `/api/auth/register` 首次注册 `admin`，应用自动创建已审核管理员；随后 `/api/auth/login` 成功签发临时 JWT。密码和 JWT 未写入日志、Apifox 持久化变量或 Git。

### 测试范围

- Apifox 项目：`8585366`
- 分支：`main`
- 本地环境：`47408671`，`http://localhost:18080`
- 当前项目接口数：17
- 当前项目已有测试用例数：11
- 管理员 JWT 仅通过本次 CLI 运行时全局变量注入，未写入 Apifox 云变量、仓库或日志。

执行方式：

```powershell
apifox test-case run <caseId> --project 8585366 --environment 47408671 --global-var "adminToken=<runtime-only>" --on-error continue
```

### 已通过用例

7 个用例通过：

- SSH test 无 Token：40100
- SSH host-key 无 Token：40100
- SSH host-key 非法指纹：40002
- SSH host-key 缺少请求体：40002
- SSH host-key 非法服务器 ID：40002
- SSH test 不存在服务器：40400
- SSH test 拒绝请求体：40002

### 未通过用例

4 个用例未通过：

- 普通用户 host-key 预期 40300，实际 40100
- host-key 禁止 CIDR 预期 40301，实际 40400
- 普通用户 SSH test 预期 40300，实际 40100
- host-key 禁止端口预期 40301，实际 40400

其中普通用户用例使用的 Token 与当前管理员运行变量不匹配，不能据此判定权限逻辑失败；禁止 CIDR/端口用例实际先命中服务器不存在或数据状态不满足前置条件，不能据此判定 SSH 出站策略失败。

### 未覆盖接口

当前 Apifox 项目没有测试用例覆盖：

- `GET /api/health`
- `GET /api/ready`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`
- `GET /api/admin/users/pending`
- `PUT /api/admin/users/{id}/approve`
- `PUT /api/admin/users/{id}/reject`
- `GET /api/servers`
- `POST /api/servers`
- `GET /api/servers/{id}`
- `PUT /api/servers/{id}`
- `DELETE /api/servers/{id}`
- `GET /api/servers/{id}/status`

注意：接口列表显示 17 个接口，但其中现有测试用例只覆盖 SSH 相关接口；本次没有宣称上述接口已通过 Apifox 验收。

### B2 PUT 目标接口

本次使用 Apifox CLI 在 `main` 分支创建了临时用例，目标为：

```http
PUT /api/servers/99999
{"description":"b2-probe"}
```

先前临时用例已删除。本轮在重启后的最新服务上重新创建临时用例 `396686204`，Schema 校验通过，并使用隔离库首次注册的管理员账号登录后获取运行时 JWT 执行成功。

```text
HTTP 404
business code 40400
```

目标断言全部通过，确认最新 `18080` 运行实例已加载 B2 修复代码。

测试完成后已删除临时 Apifox 用例 `396686204`，并删除本地临时 JSON 文件。删除前备份：

```text
C:\Backup\SuSuMonitor\execution-20260723\apifox-b2-case-cleanup-20260722-182534\
```

本次运行时 Token 未写入 Apifox 持久化变量、仓库或日志。

### 结论

本次 Apifox CLI 真实请求已完成，但结论为：

```text
已有 11 个用例：7 个通过，4 个未通过
17 个接口：未全部覆盖
B2 PUT：Apifox 授权验收通过，HTTP 404、业务码 40400、X-Request-ID 断言全部通过
```

备份：

```text
C:\Backup\SuSuMonitor\execution-20260723\apifox-api-test-20260722-182303\
```

## 十、B2-R SSH 前置条件复核

为重跑原先 4 个前置条件不匹配的用例，在隔离库中通过应用接口准备了测试数据：

- 普通用户：`apifox_user_20260722`，已审核为 `approved`，密码仅运行时注入。
- CIDR 测试服务器：ID `1`，`ssh_host=127.0.0.1`、端口 `22`。
- 端口测试服务器：ID `3`，`ssh_host=127.0.0.3`、端口 `2222`。
- 数据库：`susumonitor_metrics_cleanup_validation_20260721`。

服务器和用户均通过应用 REST 接口创建/审核，未直接写入业务表；测试数据保留在隔离库，未操作开发库。

修正并更新了 4 个 Apifox 用例，确保路径、实际服务器 ID、Token 类型和断言数组正确：

| 用例 | Apifox ID | 结果 |
|---|---:|---|
| approved user host-key returns 40300 | `396687555` | 通过，HTTP 403 / 40300 |
| approved user ssh-test returns 40300 | `396687556` | 通过，HTTP 403 / 40300 |
| forbidden CIDR returns 40301 | `396687558` | 通过，HTTP 403 / 40301 |
| forbidden port returns 40301 | `396687562` | 通过，HTTP 403 / 40301 |

每个用例均为 3 个断言全部通过：HTTP 状态、业务码和 `X-Request-ID`。

## 十一、B2-R 结论

原先 4 个未通过用例已完成前置条件修正并全部通过。此前失败原因确实是：

- 普通用户 Token 未准备，错误使用管理员 Token。
- CIDR 和端口用例引用的服务器 ID 在当前隔离库不存在。
- 原用例部分路径快照和断言数组不完整，已同步修正。

本轮没有修改 Java 业务代码、数据库迁移或前端代码。
