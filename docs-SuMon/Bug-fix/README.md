# SuSuMonitor 后端 Bug 修复目录

本目录跟踪 SuSuMonitor 后端开发过程中发现的所有 bug。
每个 bug 一份独立 Markdown 文件,命名规则 `YYYYMMDD-代号.md`。

## 当前 Bug 索引

| 日期 | 代号 | 标题 | 优先级 | 模块 | 状态 |
|---|---|---|---|---|---|
| 2026-07-21 | M4-server-list-sort-ignored | `GET /api/servers` 排序参数被忽略 | 高 | 服务器管理 | 未修复（前端用客户端排序绕过） |
| 2026-07-21 | M4-server-put-existence-check | `PUT /api/servers/{id}` 校验顺序问题(不存在也报参数错) | 中 | 服务器管理 | 代码已修复，真实 HTTP 待验证 |
| 2026-07-21 | M4-ssh-test-error-code | `POST /api/servers/{id}/ssh/test` 错误码笼统 | 中 | SSH 连接 | 未修复 |
| 2026-07-21 | M4-server-list-soft-delete | 列表过滤已软删除数据 | 低 | 服务器管理 | 已验证（J3，真实 MySQL/HTTP 通过） |

## 文档结构

每个 bug 文件包含:

1. 标题、日期、发现方式
2. 复现命令(curl / PowerShell)
3. 期望行为 vs 实际行为
4. 探测证据(响应 body、OpenAPI 字段)
5. 影响范围(前端 + 后端)
6. 建议修复方向(**不写代码,只写思路**)
7. 验收标准(修完后如何确认)

## 与前端协同

每个 bug 文件的"前端绕过方案"段说明:
- 当前如何在前端规避
- 后端修好后前端应做的撤除动作

## 排序类说明

`M4-server-list-sort-ignored` 当前用客户端排序(serverItems → sortedRows → pagedRows)绕过,
后端修好后前端应撤除该方案并恢复"参数 → 后端 → 切片"的简单流程。
