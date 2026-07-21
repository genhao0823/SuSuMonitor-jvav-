# 开发日志: Agent 与 Monitor 响应字段契约修复

**日期**: 2026-07-21

**操作人**: opencode

## 改动内容

### 修复
- 为 `AgentTokenVo` 增加 Jackson 字段映射，统一返回 `server_id`、`agent_token`、`created_at`。
- 为 `MonitorTicketVo` 增加 Jackson 字段映射，统一返回 `ticket`、`expires_at`。
- 修复 Java record 默认 camelCase 序列化与 OpenAPI、前端类型定义不一致的问题。

## 涉及文件

- `server-java-SuMon/src/main/java/com/susumonitor/server/module/server/vo/AgentTokenVo.java`
- `server-java-SuMon/src/main/java/com/susumonitor/server/websocket/MonitorTicketVo.java`

## 运行时验证

隔离实例：`http://localhost:18081`

隔离数据库：`susumonitor_agent_ws_validation_20260721`

```text
Agent fields: server_id,agent_token,created_at
Agent UTC: true
Monitor fields: ticket,expires_at
Monitor UTC: true
```

明文 Agent Token 和 Monitor Ticket 仅用于内存断言，未写入日志、开发记录或版本控制文件。

## 备份留痕

- 契约文件快照：`C:\Backup\SuSuMonitor\execution-20260721\contract-snake-case-20260721-233526\`
- 快照共 2 个文件，均已验证非空可读。

## 当前进度

Agent Token 与 Monitor Ticket 响应契约已与 OpenAPI 和前端 snake_case 类型定义一致。下一步编写可重复执行的 Agent Token REST API 自动验收脚本。
