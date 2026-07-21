# SuSuMonitor WebSocket Protocol

**Version**: 1.0

**Time standard**: UTC ISO-8601, for example `2026-07-21T12:00:00Z`

**Deployment scope**: single JVM; connection and Monitor ticket state is in memory

## Channels

```text
/ws/agent   Agent authentication, heartbeat and metrics reporting
/ws/monitor Browser metrics subscription and metrics.update delivery
```

The legacy requirement aliases `/api/ws/agent` and `/api/ws/client` map to the current Spring WebSocket paths above. Long-lived JWT and Agent Token must not be placed in a URL. `/ws/monitor` accepts only a one-time 30-second Monitor ticket obtained from `POST /api/ws/monitor-ticket`.

## Common Message

```json
{
  "type": "heartbeat",
  "message_id": "uuid",
  "timestamp": "2026-07-21T12:00:00Z",
  "payload": {}
}
```

## Agent Messages

The first Agent message must be `agent.authenticate`:

```json
{
  "type": "agent.authenticate",
  "message_id": "uuid",
  "timestamp": "2026-07-21T12:00:00Z",
  "payload": {"server_id": 1, "token": "one-time-agent-token"}
}
```

Successful authentication returns `agent.authenticated`. Authentication expires after 10 seconds if no valid first frame is received. A valid heartbeat updates `servers.last_heartbeat_at` and `agent_status=online`. No heartbeat for 90 seconds marks the Agent offline. A newly authenticated connection replaces the previous connection for the same server.

The Agent message limit is 64 KiB. Invalid JSON uses close code `1007`; oversized messages use `1009`; policy/authentication failures use `1008`.

`metrics.report` contains one fixed-width `metrics` row, including `server_id`, `collected_at`, `cpu_percent`, `memory_percent`, `memory_used`, `memory_total`, `disk_percent`, `disk_used`, `disk_total`, `net_rx`, `net_tx`, `temperature`, and `load_avg`.

## Monitor Messages

After ticket-authenticated handshake, a browser sends:

```json
{
  "type": "metrics.subscribe",
  "message_id": "uuid",
  "timestamp": "2026-07-21T12:00:00Z",
  "payload": {"server_id": 1}
}
```

`metrics.unsubscribe` removes a subscription. Duplicate subscriptions are idempotent. Only admin or approved users may subscribe to an active server.

After a committed Metrics transaction, subscribers receive:

```json
{
  "type": "metrics.update",
  "message_id": "uuid",
  "timestamp": "2026-07-21T12:00:00Z",
  "payload": {
    "server_id": 1,
    "metrics": {"cpu_percent": 35.2, "collected_at": "2026-07-21T11:59:58Z"}
  }
}
```

The broadcast never contains Agent Token, Token hash, SSH credentials, database credentials, or private keys.

## Security and Lifecycle

- Monitor ticket lifetime is 30 seconds and each ticket is consumed once.
- Ticket state is single-JVM memory only; Redis and distributed session state are not implemented.
- Metrics broadcast runs after the database transaction commits.
- Disconnect removes all subscriptions.
- Tokens and complete raw messages must not be logged.

## Runtime Validation

The following paths were validated against the isolated MySQL database `susumonitor_agent_ws_validation_20260721` and an application instance on port 18081:

```text
Agent Token REST       19 checks passed
/ws/agent              14 checks passed
/ws/monitor            16 checks passed
Flyway V1-V9           all success=1
```

The Agent validation covered authentication, heartbeat, Metrics persistence, latest/history queries and old Token rejection after rotation. The Monitor validation covered approved-user Ticket issue, subscribe, post-commit `metrics.update`, unsubscribe and single-use Ticket rejection.

The first version does not provide cross-JVM connection state, Ticket sharing, subscription routing or broadcast fan-out. These require an external state and messaging layer before multi-instance deployment.
