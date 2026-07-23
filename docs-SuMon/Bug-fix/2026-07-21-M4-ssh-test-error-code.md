# M4-ssh-test-error-code `POST /api/servers/{id}/ssh/test` 错误码笼统（已修复）

**日期**: 2026-07-21
**发现方式**: opencode 在 M4 开发期间探测 SSH test 端点
**优先级**: 中
**模块**: SSH 连接测试 API
**影响前端**: `ServerListView` / `ServerDetailView` 的"测试连接"按钮(已占位)

## 复现命令

```powershell
# 登录拿 token
$token = '<your-admin-jwt>'
$headers = @{ Authorization = ('Bearer ' + $token); 'X-Correlation-ID' = 'ssh-test-probe' }

# 任意 ID,无论是否存在
foreach ($id in @(1, 10, 99999)) {
    try {
        $r = Invoke-WebRequest -Uri "http://127.0.0.1:18080/api/servers/$id/ssh/test" `
            -Method Post -Headers $headers -UseBasicParsing -TimeoutSec 5
        Write-Host "ID=$id STATUS=$($r.StatusCode) BODY=$($r.Content)"
    } catch {
        $status = [int]$_.Exception.Response.StatusCode
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $body = $reader.ReadToEnd()
        Write-Host "ID=$id STATUS=$status BODY=$body"
    }
}
```

## 期望行为

SSH test 端点应区分以下几类错误,各自返回合适错误码:

| 场景 | 期望错误码 |
|---|---|
| SSH 服务未实现(MVP-7 未到位) | `50700` SSH feature not implemented |
| 服务器不存在 | `40400` resource not found |
| 服务器已软删除 | `40400` resource not found |
| SSH 连接超时 | `50002` ssh connection failed |
| SSH 主机指纹未登记 | `50701` host fingerprint not registered |
| 凭据错误(auth fail) | `50003` ssh authentication failed |
| 业务级 SSH 成功 | `200` + `data: { connected: true, host_key_algorithm, host_key_fingerprint, auth_type, duration_ms, tested_at }` |

## 实际行为

所有 ID 都返回:

```json
{
  "code": 50000,
  "message": "internal server error",
  "data": null
}
```

错误码笼统的 `50000 internal server error`,无法区分"功能未实现"、"服务器不存在"、"SSH 连接失败"。

## 探测证据

```
ID=1    STATUS=500  {"code":50000,"message":"internal server error","data":null}
ID=10   STATUS=500  {"code":50000,"message":"internal server error","data":null}
ID=99999 STATUS=500  {"code":50000,"message":"internal server error","data":null}
```

注: 99999 也不存在,但仍报 500 而非 404,说明 SSH test 端点**根本没做存在性检查**,直接走 SSH 尝试逻辑然后抛通用异常。

## 影响范围

### 前端

- `ServerListView` 行内和 `ServerDetailView` 头部都有"测试连接"按钮
- 当前点击触发 `ElMessage.info('测试连接功能将在 MVP-7 接入,敬请期待')` 占位,**完全跳过真实调用**
- 后端修好后再调真接口,按业务错误码分类提示

### 后端

- `SshConnectionTester` 或对应 controller 异常路径粗粒度,所有失败都返回 50000
- 缺少 `SshFeatureNotImplementedException` 等细分异常
- 全局异常处理器 `GlobalExceptionHandler` 兜底成 50000

## 建议修复方向

1. **定义新错误码**(参考 OpenAPI 错误码体系):
   - `50700` SSH feature not implemented(临时返回,功能上线后废弃)
   - `50701` SSH host fingerprint not registered
   - `50702` SSH host fingerprint mismatch
   - `50703` SSH private key passphrase incorrect
   - `50002` SSH connection failed(已有,补充语义)
   - `50003` SSH authentication failed

2. **业务异常分层**:
   - `SshFeatureNotImplementedException` → 50700
   - `SshConnectionTimeoutException` → 50002 with timeout detail
   - `SshAuthFailedException` → 50003
   - `SshHostKeyUnregisteredException` → 50701
   - 已有 `SshConnectionException` → 50002

3. **controller 入口加存在性检查**(类似 Bug 4):
   - 服务器不存在直接 40400,不进入 SSH 尝试

4. **OpenAPI 同步**: `docs-SuMon/OpenApi-SuMon/openapi-server.json` 添加上述错误码的 response 示例

## 前端绕过方案

`ServerListView.vue` / `ServerDetailView.vue` 的测试连接按钮:

```ts
function handleTestConnection(_row: Server): void {
  ElMessage.info('测试连接功能将在 MVP-7 接入,敬请期待')
}
```

**不调后端 API**,直接占位 toast。后端修好后再改为:

```ts
async function handleTestConnection(row: Server): Promise<void> {
  try {
    const response = await testSshConnection(row.id)
    if (response.data?.connected) {
      ElMessage.success(`连接成功(${response.data.duration_ms} ms)`)
    }
  } catch (error) {
    if (error instanceof ApiBusinessError) {
      if (error.code === ErrorCode.SSH_NOT_IMPLEMENTED) {
        ElMessage.warning('功能开发中')
      } else if (error.code === ErrorCode.RESOURCE_NOT_FOUND) {
        ElMessage.error('服务器不存在')
      } else {
        ElMessage.error(`连接失败: ${error.message}`)
      }
    }
  }
}
```

## 后端修好后前端改动

1. 新增 `src/api/server.ts` 的 `testSshConnection(id: number)` 函数
2. 新增 `src/types/api.d.ts` 的 `SshTestResult` 接口
3. 修改 `ErrorCode` 加入新错误码
4. `ServerListView` / `ServerDetailView` 的 `handleTestConnection` 改为真实调用

## 验收标准

后端修完后:

- [ ] `POST /api/servers/99999/ssh/test` 返回 `40400`(不存在)
- [ ] `POST /api/servers/{existing_id}/ssh/test` 未实现时返回 `50700`
- [ ] `POST /api/servers/{existing_id}/ssh/test` SSH 失败返回 `50002`
- [x] `POST /api/servers/{existing_id}/ssh/test` auth 失败返回 `50003`
- [ ] 成功响应 body 符合 OpenAPI `SshTestVo` schema
- [x] OpenAPI 文档同步更新错误码表

## J4 实现与验证结果（2026-07-23）

本轮保留现有正式契约 `50400 ssh connection timeout`，仅新增 `50003 ssh authentication failed`。

实现变更：

- sshj `UserAuthException` 单独映射为 `AUTHENTICATION_FAILED`，避免被通用 `IOException` 归类为连接失败。
- `ServerSshService` 将该分类映射为 `50003`，HTTP 状态保持 `502 Bad Gateway`。
- 不存在、软删除、未确认指纹、出站策略拒绝、连接失败、超时和并发限制等既有错误码未改变。

自动化验证：

| 验证项 | 结果 |
|---|---|
| Service 分类映射 | 覆盖 `AUTHENTICATION_FAILED -> 50003` |
| Controller HTTP 映射 | `50003 -> HTTP 502` |
| 超时契约 | 保持 `50400` |
| OpenAPI | 错误码枚举和 502 示例已包含 `50003` |

## J5 真实 SSH 验收结果（2026-07-23）

已复用 WSL Ubuntu 24.04.4 的受控 OpenSSH 服务，监听 `2222`；创建专用用户 `susu-ssh-j5-20260723`。开发库测试服务器为唯一记录 ID `39`，删除前已备份：

```text
C:\Backup\SuSuMonitor\java-backend-closure-20260723\J5-susumonitor-before-ssh-apifox-20260723.sql
```

TCP 连接、SSH KEX 和 Java 出站策略均已通过，但 sshj 主机密钥握手返回 `50002`，未成功登记主机指纹，因此未到达用户认证阶段，不能将本轮记为真实 `50003` 通过。使用实际 ED25519 和 RSA 指纹分别重试后仍为 `50002`，需后续排查 sshj 0.39.0 与当前 OpenSSH 主机密钥协商兼容性。

测试服务器 ID `39` 随后仅通过业务接口软删除：HTTP 200、业务码 0；数据库行保留且 `deleted=1`、`deleted_at` 非空、`delete_token` 非空。未执行物理删除、TRUNCATE、DROP 或数据库重置。
