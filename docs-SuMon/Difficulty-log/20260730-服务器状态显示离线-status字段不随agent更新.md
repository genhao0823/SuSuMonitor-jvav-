# 2026-07-30 服务器状态显示离线(status 字段不随 agent 在线更新)

**日期**: 2026-07-30
**操作人**: opencode / 用户
**关联**: WSL agent 已鉴权并每 5 秒上报指标(DB 可见),但前端服务器列表"状态"列显示离线

## 一、Bug 现象

WSL agent 部署成功:
- agent 日志 `agent authenticated server_id=4`
- 后端 DB `metrics` 表 server_id=4 每 5 秒一条新记录(采集上报闭环)
- 但前端 `/servers` 列表"状态"列显示**离线**(应有"在线")
- "Agent"列显示 `online`(正确)

## 二、尝试的方法

1. **怀疑 agent 没真正连上**:查 agent.log,确认 `agent authenticated` + `metrics reported` 每 5 秒成功;查 nginx access log,`/ws/agent` 101 连接保持——agent 确实在线。
2. **查 DB agent_status**:`SELECT agent_status FROM servers WHERE id=4` 返回 `online`——agent 在线状态正确更新了。
3. **发现 status 与 agent_status 是两个字段**:DB 查询显示 `status=offline` 但 `agent_status=online`。前端 `ServerListView` 有两列:
   - "状态"列显示 `row.status`(=offline → 离线)
   - "Agent"列显示 `row.agent_status`(=online)
4. **grep 后端是否更新 status**:`grep setAgentStatus/updateAgentStatus` 全工程无调用——**后端从不更新 status 字段**。

## 三、根因

`ServerMapper.xml` 的 `updateAgentHeartbeat`(agent 心跳/鉴权时调)只更新 `agent_status='online'`,**不更新 `status`**:
```xml
<!-- 修复前 -->
<update id="updateAgentHeartbeat">
    UPDATE servers
    SET agent_status = 'online',
        last_heartbeat_at = #{heartbeatAt}
    WHERE id = #{serverId} ...
</update>
```

`status` 字段创建服务器时默认 `offline`,`updateAgentHeartbeat`/`markAgentOffline`/`revokeAgentToken` 都不更新它,导致 `status` 永远是 `offline`。

而 `ServerServiceImpl.status()` 注释"仅返回数据库快照",从 DB 读 `status` 返回前端——前端"状态"列永远显示离线。

`status` 语义就是"在线/离线"(`serverStatusLabel`: online→在线, offline→离线),应跟随 agent 连接状态,但后端没打通。

## 四、修复

`ServerMapper.xml` 三处 SQL 同步更新 `status`:

```xml
<!-- 心跳/鉴权成功:status 同步 online -->
<update id="updateAgentHeartbeat">
    UPDATE servers
    SET agent_status = 'online',
        status = 'online',
        last_heartbeat_at = #{heartbeatAt}
    WHERE id = #{serverId} ...
</update>

<!-- 心跳超时/断开:status 同步 offline -->
<update id="markAgentOffline">
    UPDATE servers
    SET agent_status = 'offline',
        status = 'offline'
    WHERE id = #{serverId} ...
</update>

<!-- 撤销 token:status 同步 offline -->
<update id="revokeAgentToken">
    UPDATE servers
    SET agent_token_revoked_at = #{revokedAt},
        agent_status = 'offline',
        status = 'offline'
    WHERE id = #{serverId} ...
</update>
```

## 五、验证

本地 Maven 重新构建后端 jar → 部署到云服务器新 release 目录 `cloud-20260730` → 切 symlink → `systemctl restart susumonitor-server`。

agent 重连鉴权后查 DB:
```
id=4 name=wsl111  status=online  agent_status=online  last_heartbeat_at=03:23:20
```
前端刷新 `/servers`,server #4 "状态"列显示"在线"。

## 六、教训

1. **两个字段同义但不同步是隐蔽 Bug**:`status` 和 `agent_status` 语义都是"在线/离线",但只有 `agent_status` 被心跳更新,`status` 被遗忘。设计上要么合并,要么明确各自语义并同步。
2. **前端显示哪个字段要和后端更新逻辑对齐**:前端"状态"列读 `status`,后端却只更新 `agent_status`——两边对字段的职责理解不一致。
3. **"仅返回数据库快照"的接口要确保快照被正确维护**:`status()` 声明只读 DB,但若 DB 字段从不被写,快照就是错的。
