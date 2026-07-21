# 开发日志: Agent Token REST 真实验收

**日期**: 2026-07-21

**操作人**: opencode

## 改动内容

### 新增
- 新增 `api-test/verify-agent-api.ps1`，使用 PowerShell 5.1 对 Agent Token REST API 执行可重复运行的真实验收。
- 脚本通过环境变量接收管理员和数据库验证凭据，不写入源码、日志或提交记录。
- 测试服务器使用时间戳生成唯一名称和 `127.1.x.x` 回环地址，避免重复执行触发唯一索引冲突。

## 验收范围

- 管理员登录并创建隔离测试服务器。
- 首次注册 Agent Token。
- Agent Token 响应字段为 `server_id`、`agent_token`、`created_at`，时间为 UTC。
- 数据库仅保存 `sha256:` 哈希，不保存明文 Token。
- Agent 尚未建立 WebSocket 连接时状态保持 `offline`。
- 重复注册返回 HTTP 409 / 业务码 40900。
- 轮换 Token 后新旧值不同。
- 撤销成功，重复撤销返回 HTTP 409 / 业务码 40900。
- 未认证请求返回 HTTP 401 / 业务码 40100。
- 不存在服务器返回 HTTP 404 / 业务码 40400。
- 非正数服务器 ID 返回 HTTP 400 / 业务码 40002。

## 涉及文件

- `api-test/verify-agent-api.ps1`
- `docs-SuMon/Develop-log/20260721-Agent-Token-REST真实验收.md`

## 实际结果

```json
{"status":"PASS","token_values_logged":false,"server_id":5,"checks":19}
```

验证层级：真实 HTTP + 真实 MySQL 隔离库。尚不代表 `/ws/agent` 或 `/ws/monitor` 已通过。

## 执行留痕

- 隔离实例：`http://localhost:18081`
- 隔离数据库：`susumonitor_agent_ws_validation_20260721`
- 隔离数据库继续保留，未执行删除。
- 脚本创建的是隔离验收数据，不修改开发库 `susumonitor`。

## 问题与修正

- 首次脚本使用 PowerShell 多行 `-or` 写法，PowerShell 5.1 解析失败；改为兼容换行格式。
- 固定 `127.0.0.1` 触发服务器 host 唯一约束；改为时间戳派生的唯一回环地址。
- `Invoke-RestMethod` 的错误响应体读取不稳定；改为 `HttpWebRequest`，稳定读取 HTTP 状态和统一 JSON 错误体。
- Authorization 必须在获取请求体流前设置；调整 header 写入顺序后通过。
