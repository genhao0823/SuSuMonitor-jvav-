# SuSuMonitor RabbitMQ 运维手册（MVP-8）

**适用范围**：MVP-10 Outbox 引入的 RabbitMQ 依赖的日常运维。
**相关**：`Develop-log/20260731-MVP10-Metrics-Outbox.md`（实施与验收）、`docs-SuMon/Protocol-SuMon/rabbitmq-topology-v1.md`（冻结拓扑）。

---

## 一、架构位置（为什么需要 RabbitMQ）

```text
Agent 上报 metrics.report
  -> MySQL 事务：指标落库 + message_outbox(pending) 同事务写入
  -> OutboxPublisherScheduler（1s 轮询，FOR UPDATE SKIP LOCKED）
  -> Publisher Confirm 投递到 susumonitor.events
  -> susumonitor.alert.metrics 队列（MVP-11 接入消费者；当前堆积为预期）
```

- RabbitMQ 只承载 **metrics.reported.v1 事件投递**；Agent/Monitor WebSocket、MySQL、SSH/终端链路均不经过它。
- **Broker 停机不影响指标采集与落库**，只影响事件投递（outbox 退避堆积，恢复后自动补发）。

## 二、vhost / 用户 / 权限管理

```bash
# 查看
rabbitmqctl list_vhosts
rabbitmqctl list_users
rabbitmqctl list_permissions -p susumonitor

# 创建（部署安装手册 §3.2）
rabbitmqctl add_vhost susumonitor
rabbitmqctl add_user susumonitor '<强密码>'
rabbitmqctl set_permissions -p susumonitor susumonitor '.*' '.*' '.*'
rabbitmqctl set_user_tags susumonitor administrator    # 管理台/验收需要；生产可收回为 monitoring

# 轮换密码（需同步更新 server.env 的 SPRING_RABBITMQ_PASSWORD 并重启后端）
rabbitmqctl change_password susumonitor '<新密码>'

# 删除/禁用
rabbitmqctl delete_user <用户名>
rabbitmqctl clear_permissions -p susumonitor <用户名>
```

> 凭据管理：RabbitMQ 密码通过 `server.env` 的 `SPRING_RABBITMQ_PASSWORD` 注入；vhost/用户清单建议随《备份与恢复手册》记录。

## 三、拓扑（冻结 4 件套，幂等声明）

| 类型 | 名称 | 关键参数 |
|---|---|---|
| Topic Exchange | `susumonitor.events` | durable, non-auto-delete |
| Topic Exchange | `susumonitor.dlx` | durable, non-auto-delete |
| Queue | `susumonitor.alert.metrics` | durable；`x-dead-letter-exchange=susumonitor.dlx`、`x-dead-letter-routing-key=metrics.reported.v1` |
| Queue | `susumonitor.alert.metrics.dlq` | durable，不自动回投业务队列 |

- 由 `RabbitMqTopologyConfig` 声明式创建，**broker 重启/升级后自动重建**，无需手工声明。
- 绑定：`susumonitor.events -- metrics.reported.v1 --> susumonitor.alert.metrics`；DLX → DLQ 同理。

```bash
rabbitmqctl list_queues -p susumonitor name durable arguments
rabbitmqctl list_exchanges -p susumonitor name type durable
```

## 四、健康检查与监控

| 项 | 方式 | 说明 |
|---|---|---|
| 后端视角 | `GET /api/ready` | DB + RabbitMQ 双检查；Broker 不可达返回 **HTTP 503 / 50301 "rabbitmq unavailable"**（存活但未就绪，应用不退出） |
| 积压监控 | `rabbitmqctl list_queues -p susumonitor name messages` | `susumonitor.alert.metrics` 消息数 = 待 MVP-11 消费的堆积量（当前增长属预期）；**异常突增**（发布速率远高于指标速率）需排查 |
| 管理台 | http://127.0.0.1:15672（仅内网） | 队列/连接/节点监控 |
| Broker 状态 | `rabbitmqctl status` | 节点/版本/Erlang 版本 |

## 五、停机影响矩阵与恢复

| 场景 | 影响 | 恢复行为 |
|---|---|---|
| Broker 停机 | 指标照常落库；outbox 事件退避堆积（指数退避封顶 300s）；`/api/ready` 50301 | Broker 恢复后发布器**自动补发**，无需人工干预（MVP-10 三阶段验收 PASS 证据） |
| Broker 数据目录损坏 | 消息丢失（业务队列堆积的事件）；**指标数据不受影响**（已落 MySQL） | 重建 broker → 拓扑自动声明 → 后续事件正常投递；丢失的堆积事件需接受（消费侧未启动前无业务损失） |
| 后端重启 | 无影响 | 发布器随应用恢复轮询；Broker 侧连接自动重连 |

## 六、生产部署建议

1. **系统服务方式**（官方 Windows 服务 / Linux 包管理安装），**不要用本机验收的免安装前台方式**（`local/rmq` 仅限开发机验收，`Develop-log/20260731-MVP10-Metrics-Outbox.md` 已声明）。
2. **版本匹配**：RabbitMQ 4.3 必须配 Erlang 27.x；**Erlang 29 不兼容**（horus 机制启动失败，实测记录）。升级前查官方版本矩阵。
3. 备份与恢复：RabbitMQ 配置/用户清单按《备份与恢复手册》记录；消息队列本身不做业务级备份（指标数据在 MySQL，消息是短暂的投递载体）。
