# M4-server-list-soft-delete 列表过滤已软删除数据（已验证）

**日期**: 2026-07-21
**发现方式**: opencode 在 M4 开发期间推断
**优先级**: 低
**模块**: 服务器管理 API
**影响前端**: `ServerListView` 数据完整性

## 待确认的问题

`GET /api/servers` 列表接口是否过滤了已软删除(deleted=1)的服务器记录?

按 OpenAPI 描述与数据库设计,服务器采用软删除(`servers.deleted` 字段),`deleted=1` 的记录不应出现在列表中。但当前前端探测只确认了"18 条样本存在",**未确认是否有已软删除记录被排除**。

## 探测方法

```powershell
# 登录拿 token
$token = '<your-admin-jwt>'
$headers = @{ Authorization = ('Bearer ' + $token) }

# 1. 直接查数据库,统计 deleted=0 和 deleted=1 各多少
#   这需要后端配合或 SQL 直查
# 2. 软删除某台服务器
#   DELETE /api/servers/{some_id}
# 3. 立即 GET /api/servers 看 total 是否减少
$r1 = Invoke-WebRequest -Uri 'http://127.0.0.1:18080/api/servers?page=1&page_size=999' `
    -Method Get -Headers $headers -UseBasicParsing
$before = ($r1.Content | ConvertFrom-Json).data.total

# 选一台真存在的服务器软删除
$targetId = 32  # 替换成实际 ID
Invoke-WebRequest -Uri "http://127.0.0.1:18080/api/servers/$targetId" `
    -Method Delete -Headers $headers -UseBasicParsing

$r2 = Invoke-WebRequest -Uri 'http://127.0.0.1:18080/api/servers?page=1&page_size=999' `
    -Method Get -Headers $headers -UseBasicParsing
$after = ($r2.Content | ConvertFrom-Json).data.total

Write-Host "删除前 total=$before, 删除后 total=$after"
```

## 期望行为

`$after` 应等于 `$before - 1`,被软删除的服务器不再出现在列表中。

## 待观察的实际行为

| 场景 | 期望 | 可能实际 |
|---|---|---|
| 删除后列表 total 减少 | total = before - 1 | 取决于后端是否过滤 deleted=1 |
| 删除后 GET /api/servers/{deleted_id} | 40400 | 取决于是否走 deleted 过滤 |
| 删除后 GET /api/servers/{deleted_id}/status | 40400 | 同上 |

## 影响范围

### 前端

- 当前未做特殊处理,直接展示后端返回的数据
- 若后端不过滤,前端会显示已软删除的服务器,可点击"详情"会跳回列表(404 处理)
- 用户体验差但不影响数据完整性

### 后端

- `ServerController.list` 可能没加 `deleted = 0` 过滤条件
- `ServerService.list` / Mapper XML 可能没加 `<where deleted = 0</where>`
- 需要实测确认

## 建议修复方向

后端 controller / service / mapper 任一层加过滤:

```java
// service 或 mapper
QueryWrapper<Server> qw = new QueryWrapper<>();
qw.eq("deleted", 0);  // 排除软删除
qw.orderBy(...);
```

或显式 SQL `WHERE deleted = 0`。

## J3 验证结果（2026-07-23）

本轮只在开发库创建唯一前缀测试服务器 `j3-soft-delete-validation-20260723`，目标 ID 为 `37`。软删除前已生成并验证可读取的开发库备份：

```text
C:\Backup\SuSuMonitor\java-backend-closure-20260723\J3-susumonitor-before-soft-delete-20260723.sql
```

该服务器随后仅通过业务接口执行软删除，未执行物理删除、TRUNCATE、DROP 或数据库重置。删除前后数据库计数为 `active=21/deleted=11`、`active=21/deleted=12`。

| 验证项 | 实际结果 |
|---|---|
| DELETE `/api/servers/37` | HTTP 200，业务码 0 |
| 列表 `total` | 21，与数据库 `deleted=0` 数量一致；ID 37 不出现 |
| 详情 | HTTP 404，业务码 40400 |
| 状态 | HTTP 404，业务码 40400 |
| 更新 | HTTP 404，业务码 40400 |
| SSH test | HTTP 404，业务码 40400 |
| 数据库记录 | 仍保留，`deleted=1`、`deleted_at` 非空、`delete_token` 非空 |

自动化验证：Controller/Service 定向测试 `72/72` 通过；真实 MySQL Mapper IT `5/5` 通过；常规回归 `196/196` 通过；Java 编译通过。真实 HTTP 每个响应均存在 `X-Request-ID`。

**OpenAPI 同步**:

`docs-SuMon/OpenApi-SuMon/openapi-server.json` 的 `GET /api/servers` 描述应明确:
- 响应**不**包含已软删除的服务器
- 如需看已软删除的服务器,后续可加 `?include_deleted=true`(默认 false)

## 前端绕过方案

**目前不需要绕过**——前端直接展示后端数据。若后端确实不过滤:

- 选项 A:前端按 `deleted === false` 过滤再渲染(依赖 Server VO 是否含 deleted 字段)
- 选项 B:等待后端修

**当前 Server VO 不含 `deleted` 字段**(查看 `types/api.d.ts` 和 OpenAPI),所以选项 A 也需要后端配合加字段。

## 后端修好后前端可选优化

如需在 UI 上让用户看到已删除服务器,可:
- 加 `include_deleted` 查询参数
- `Server` VO 加 `deleted: boolean` 字段
- 列表加"显示已删除"勾选框(高级功能,本期不做)

## 验收标准

后端修完后:

- [x] 软删除后列表 total 立即减少
- [x] GET 已软删除 ID 返回 40400
- [x] GET 已软删除 ID 的 status 端点也返回 40400
- [ ] OpenAPI 文档明确说明列表排除 deleted=1
- [x] 数据库直查 `SELECT COUNT(*) FROM servers WHERE deleted=0` 与 API 返回的 total 一致
