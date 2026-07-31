# 2026-07-30 Agent 断开后服务器仍显示在线(afterConnectionClosed 不更新 DB)

**日期**: 2026-07-30
**操作人**: opencode / 用户
**关联**: WSL agent 关机后,前端服务器列表 wsl111(#4) 仍显示"在线"

## 一、Bug 现象

用户关闭 WSL(agent 进程退出,/ws/agent 断开)后:
- 前端 `/servers` 列表里 wsl111(#4) **状态列仍显示"在线"**
- 刷新页面也不变(一直在线)
- 实际 agent 已离线,不再上报指标

## 二、尝试的方法

1. **查 DB 确认残留**:查 `servers` 表 id=4,`status=online`、`agent_status=online`、`last_heartbeat_at` 停在关机前最后一次心跳--DB 确实没更新为 offline。
2. **回顾之前的修复**:之前修了 `updateAgentHeartbeat`/`markAgentOffline`/`revokeAgentToken` 的 `status` 同步(`markAgentOffline` SQL 有 `status='offline'`)--SQL 层没问题。
3. **查 `markAgentOffline` 何时被调**:读 `AgentHeartbeatServiceImpl`,发现只有两处调 `markAgentOffline`:
   - `heartbeat()` 里不调(只 updateAgentHeartbeat)
   - `markExpiredSessionsOffline()`(@Scheduled 30s)扫 `connectionRegistry.sessions()`,超时(90s 未心跳)的才调
4. **定位断开处理**:读 `AgentWebSocketHandler.afterConnectionClosed`(agent /ws/agent 断开时回调),发现它**只移除内存 `connectionRegistry.remove(session)`,不更新 DB**:
   ```java
   // 修复前
   if (session != null && session.authenticated()) {
       if (terminalRelayLifecycleService != null) {
           terminalRelayLifecycleService.closeAgentSessions(session);
       }
       connectionRegistry.remove(session);  // ← 只清内存,DB status 不动
   }
   ```
5. **为什么超时扫描也扫不到**:agent 关机后 `/ws/agent` 断开,`afterConnectionClosed` 把 session 从 `connectionRegistry` 移除了。`markExpiredSessionsOffline` 扫 `connectionRegistry.sessions()`,session 已不在,扫不到,不会调 `markAgentOffline`--**断开的 agent 成了 DB 里的"孤儿 online"**。

## 三、根因

`AgentWebSocketHandler.afterConnectionClosed`(agent 连接断开回调)**只清内存 registry,不更新 DB**。

- agent 正常心跳时:`updateAgentHeartbeat` 设 `status=online`(已修)
- agent 断开时:`afterConnectionClosed` 只 `connectionRegistry.remove`,**不设 `status=offline`**
- 超时扫描 `markExpiredSessionsOffline`:扫 `connectionRegistry.sessions()`,但断开的 session 已移除,**扫不到**,不会补救设 offline

结果:agent 关机/进程退出后,DB `status` 保持上一次心跳设的 `online`,前端永远显示在线。

## 四、修复(3 个文件)

### 1. `AgentHeartbeatService` 接口加方法

```java
/**
 * Agent 连接断开时标记对应服务器离线。
 * 使用乐观锁:仅当 DB last_heartbeat_at 仍是断开时的值才设 offline,
 * 防止误把已重连新连接(新心跳已更新 last_heartbeat_at)设为离线。
 */
void markOfflineOnDisconnect(AgentWebSocketSession session);
```

### 2. `AgentHeartbeatServiceImpl` 实现

```java
@Override
public void markOfflineOnDisconnect(AgentWebSocketSession session) {
    if (session == null || !session.authenticated() || session.serverId() == null
            || session.lastHeartbeatAt() == null) {
        return;
    }
    // 乐观锁:仅当 last_heartbeat_at 仍是断开时的值才设 offline,
    // 防止误把已重连新连接(新心跳更新了 last_heartbeat_at)设为离线。
    serverMapper.markAgentOffline(session.serverId(), session.lastHeartbeatAt());
}
```

复用已有的 `markAgentOffline` SQL(之前修过,带 `status='offline'` + `WHERE last_heartbeat_at = #{expectedHeartbeatAt}` 乐观锁)。

### 3. `AgentWebSocketHandler.afterConnectionClosed` 调用

```java
if (session != null && session.authenticated()) {
    if (terminalRelayLifecycleService != null) {
        terminalRelayLifecycleService.closeAgentSessions(session);
    }
    // Agent 断开(关机/进程退出)时立即标记服务器离线;
    // 乐观锁防止误覆盖已重连新连接。
    if (heartbeatService != null) {
        heartbeatService.markOfflineOnDisconnect(session);
    }
    connectionRegistry.remove(session);
}
```

### 乐观锁防误覆盖

`markAgentOffline` SQL 的 `WHERE last_heartbeat_at = #{expectedHeartbeatAt}` 是关键:
- agent 真断开(不重连):DB `last_heartbeat_at` 仍是断开时的值 -> 匹配 -> 设 offline ✓
- agent 被替换重连(旧连接 afterConnectionClosed 时新连接已心跳):DB `last_heartbeat_at` 已是新值 -> 不匹配 -> **不设 offline**(避免误把新在线连接设离线)✓

## 五、验证

1. 重新构建后端 jar -> 部署 cloud-20260730 release -> `systemctl restart`
2. 手动清理 server 4 残留 online(旧版后端没设 offline 的 bug 状态):
   ```sql
   UPDATE servers SET status='offline', agent_status='offline' WHERE id=4;
   ```
   before: `status=online` -> after: `status=offline` ✓
3. 用户验证流程:
   - 重启 WSL agent -> 连上后端 -> `updateAgentHeartbeat` 设 online -> 前端显示在线
   - 关 WSL -> /ws/agent 断开 -> `afterConnectionClosed` 调 `markOfflineOnDisconnect` -> DB 设 offline -> 前端显示离线

## 六、教训

1. **连接断开回调要同步清理 DB**:WebSocket 断开(`afterConnectionClosed`)只清内存 registry 是不够的,还要更新 DB 状态(否则前端读 DB 快照永远是旧值)。内存状态和 DB 状态要一致。
2. **超时扫描扫不到已移除的 session**:`markExpiredSessionsOffline` 扫 `connectionRegistry.sessions()`,但 `afterConnectionClosed` 已移除断开的 session,扫描扫不到--**不能只靠超时扫描兜底,断开时要主动设 offline**。
3. **乐观锁防误覆盖是关键**:agent 重连场景(旧连接断开 + 新连接已心跳)下,直接 `UPDATE status=offline` 会误把新在线连接设离线。用 `WHERE last_heartbeat_at = 断开时的值` 乐观锁,只有没被新心跳覆盖才设 offline--和 `replace`(新连接踢旧连接)配合,保证只有"真离线"才设 offline。
4. **和"status 字段不更新"是一对 bug**:之前修了"心跳时不更新 status"(只更新 agent_status),这次修了"断开时不更新 status"(afterConnectionClosed 不更新 DB)。两个加起来,status 才能完整跟随 agent 的 online->offline 生命周期。修第一个时没意识到断开路径也没更新,导致复发。
