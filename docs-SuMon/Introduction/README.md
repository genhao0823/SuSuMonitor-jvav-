# SuSuMonitor 项目介绍（Java 实习面试材料）

> **文档目的**：将 `SuSuMonitor(Jvav)` 项目按"面试场景"重新组织成 6 份可独立翻阅的 Markdown，作为 Java 实习 / 校招面试前的"项目讲解脚本 + 速查手册"。
>
> **目标读者**：面试候选人（你）+ 面试官。
>
> **配套资料**：本目录 6 份 md 互相独立，可单独抽 1 份打印带进面试间。
>
> **写作日期**：2026-07-31（与项目最新 commit `0acb3b1` 对齐）。
>
> **状态约定**：与根目录 `README.md` 一致——`已实现` / `已验证` / `未验证` / `未实现` / `计划中`。

---

## 一、一句话定位

**SuSuMonitor 是一套"前后端 + Agent"全栈 Linux 服务器性能监控平台**：Vue 3 提供 Web 控制台、Java 21 + Spring Boot 3 做后端 + WebSocket 网关、Go Agent 跑在被监控机器上采集指标并提供反向 PTY 终端，主打"实时链路 + 告警闭环 + Web 终端"三件套。整套系统由我（个人开发者）从 0 到 1 完成，并已经本机闭环跑通 MVP-1~MVP-7 全部业务模块，全部 commit 可见、可在隔离 MySQL 库 + WSL 受控 SSHD 环境下真实验证。

---

## 二、核心数据（开场讲这两组数字就够）

| 维度 | 数据 | 来源 |
|---|---|---|
| Git 提交 | 65+ commits，Tag `v0.4.0-sprint4` | `git log --oneline` |
| 模块 | 后端 25+ 业务类、Agent 8 个 internal 子包、前端 13 个视图 + 21 个组件 | 各模块 README 计数 |
| 测试 | 320+ 单元测试（JUnit 5 + Vitest）+ 4 道防线（Vitest / audit:catchup / api:e2e / ui:e2e） | `audit:catchup` 报告 |
| 文档 | 67+ 开发日志、5 篇困难记录、9 篇 bug 修复记录、6 个 OpenAPI 契约、6 份技术总结 | `docs-SuMon/` |
| 里程碑 | MVP-1 后端基线 → MVP-2 Agent → MVP-3 实时链路 → MVP-5A Web → MVP-6 告警 → MVP-7 终端 | `Develop-plans/` |
| 真实验证 | Apifox 27 个断言、WSL 受控 SSHD `127.0.0.1:2223`、隔离 MySQL 库、浏览器 IAB 真实联调 | `2026072*-Apifox-*`、`20260726-终端Java-Go真实联调*` |

---

## 三、6 份文档导航 + "何时翻这份"

| 文件 | 字数 | 何时翻 |
|---|---|---|
| **`README.md`（本文件）** | 2k | 面试开场的 60 秒自我介绍 + 高频问题速查 |
| **`01-业务功能.md`** | 10k | 面试官问"做了什么" / "具体怎么实现的"——每模块配字段表 + 流程步骤 + API 表 |
| **`02-技术栈与架构.md`** | 5k | 面试官问"用了什么技术、为什么这么选、整体架构如何"——4 张栈表 + ASCII 架构图 + 8 条设计权衡 |
| **`03-核心技术难点与解决方案.md`** | 12k | ⭐ **面试加分项**：面试官问"最有挑战的部分 / 解决过什么棘手的问题"——8 个精选难题配 5 段式（问题→根因→方案→落地→收获） |
| **`04-项目里程碑与真实验证.md`** | 4k | 面试官质疑"这只是 demo"——按时间线展示 MVP-1→MVP-7 真实闭环证据 + Apifox/WSL/隔离库证据链 |
| **`05-项目反思与未来规划.md`** | 3k | 面试官问"有什么不足 / 未来怎么演进 / 你的成长方向"——已实现/未验证/计划中三段 + 演进路径 |

---

## 四、60 秒电梯演讲（背诵备用）

> "我做的是一套 Linux 服务器监控平台 SuSuMonitor。**后端**用 Java 21 + Spring Boot 3 + MyBatis-Plus 做 REST + WebSocket 双网关，凭 JWT 鉴权用户、一次性 ticket 鉴权浏览器，事件驱动做实时推送；**前端** Vue 3 + Element Plus + xterm.js 实现管理面板和 Web SSH 终端；**Agent** 用 Go 1.23 + gopsutil + creack/pty 跑在被监控机器上，按 5 秒 tick 上报 CPU / 内存 / 磁盘 / 温度 / 负载。**架构上三个最有挑战的点**——一是告警状态机用了 sealed interface 4 状态 + 乐观锁 + 唯一索引三件套保证一致性和幂等；二是反向 Web 终端走三段链路双层字节限流 + Monitor 背压保护防止一端卡死整条崩；三是指标用 UUID 幂等 + 行锁 + 单调时钟校验双层防御解决 Agent 重连和时钟回拨。整套代码在隔离 MySQL + WSL SSHD + 真实浏览器都跑通过 MVP-1 到 MVP-7 全量业务。"

---

## 五、面试高频问题速查表（按出现频率）

| 面试问题 | 答什么（直链） | 关键类 / 文档 |
|---|---|---|
| 这个项目做了什么？ | `01-业务功能.md` 八模块全表 | — |
| 为什么用 Spring Boot 3 + Java 21？ | `02-技术栈与架构.md` §Java 21 + §设计权衡 | `pom.xml:1-26` |
| WebSocket 鉴权怎么做的？ | `01-M3` Agent 通道 + `01-M4` Monitor 通道 | `AgentAuthenticationServiceImpl` / `MonitorTicketServiceImpl` |
| JWT 还是 Session？为什么？ | `01-M1` + `02-§Ch6 设计权衡` | `SecurityConfig.java` + `JwtAuthenticationFilter.java` |
| 告警怎么保证不漏不重？ | `03-§难题 2` 三件套 | `AlertStateMachine` + V13 SQL |
| Agent 离线怎么检测？ | `01-M3` + `03-§难题 3` | `AgentHeartbeatServiceImpl` + `markOfflineOnDisconnect` |
| 实时数据怎么推送？ | `01-M4` + `03-§难题 5` | `MonitorMetricsPublisher` AFTER_COMMIT |
| Web 终端原理？ | `01-M6` + `03-§难题 1` | `TerminalWebSocket` + `creack/pty` |
| 监控数据怎么清理？ | `01-M4` + `02-§运维栈` | `MetricsCleanupServiceImpl` + `@Scheduled` |
| 数据库怎么迁移？ | `01-M8` | Flyway V1-V13 (`db/migration/`) |
| 测试怎么做的？ | `04-§当前测试数据` | JUnit5 + Vitest + H2 + *IT.java |
| 部署怎么做的？ | `01-M8` + `04-§T5 部署` | systemd + Nginx + install.sh |
| 这个项目最难的部分？ | `03-§难题 1~8` | 8 个五段式难题 |
| 还有什么没做完？ | `05-§未验证` + `§计划中` | MVP-10/11 + Docker/Android |
| 你的下一步学习方向？ | `05-§个人成长方向` | — |
| 你做这个项目最大的收获？ | `03-§每难题的收获` + `05-§反思` | — |

---

## 六、关键术语对照（项目术语 ↔ 通用概念）

| 项目术语 | 通俗解释 | 出处 |
|---|---|---|
| **Agent** | 安装在被监控 Linux 上的 Go 进程，负责采集 + WS 上报 + 反向 PTY | `agent-go-SuMon/` |
| **Monitor（监控端）** | 用户浏览器侧，长连 `/ws/monitor`，只收推送不主动采集 | `MonitorWebSocketHandler` |
| **Monitor Ticket** | 用户先调 REST 拿到的 32B 一次性 30s 凭证，用作 WS 握手 ticket | `MonitorTicketServiceImpl` |
| **Agent Token** | 每个服务器一个的 32B Base64Url 凭证，Agent 用它首帧鉴权；存 DB 用 SHA-256 哈希 | `AgentTokenServiceImpl` |
| **AFTER_COMMIT 事件** | Spring `TransactionPhase.AFTER_COMMIT` 钩子：只在事务真正提交后才广播副作用 | `MetricsReportedEvent` 触发链路 |
| **AlertStateMachine** | sealed interface 4 种 transition：Trigger / ContinueBreached / Resolve / NoAction | `AlertStateMachine.java` |
| **PTY** | Pseudo-Terminal，Linux 内核暴露的伪终端设备，让 Go Agent 给 Web 终端做反向 shell | `creack/pty` |
| **Monitor 背压** | 浏览器侧慢消费时，Spring 用 `ConcurrentWebSocketSessionDecorator` 主动断连防雪崩 | `MonitorWebSocketSession` |
| **三段链路** | `浏览器 WS ↔ Java 中继 ↔ Agent WS ↔ Linux PTY`，WSS 不直接端到端 | `TerminalAgentRelayService` + `TerminalMonitorRelayService` |
| **MVP 阶段** | Milestone Phase，对应 `Develop-plans/` 中的规划（1-11）+ 后置 Polish | `Develop-plans/20260712项目规划.md` |
| **Sprint 1-4** | MVP-5A 后的快速迭代，1 周/阶段，分别接 SSH / spark / 清理 | `docs-SuMon/Develop-log/2026072[2-5]-*` |

---

## 七、面试讲解节奏建议（5 / 15 / 30 分钟三档）

| 面试时长 | 讲解路径 | 关键素材 |
|---|---|---|
| **5 min（自我介绍）** | 本文件 §四 电梯演讲 + `01` 八模块名称 | 1 张纸，能讲出技术亮点即可 |
| **15 min（项目深挖）** | 电梯演讲 → 业务功能精讲 2 个亮点模块（推荐 M3 Agent + M5 告警）→ 1 个最难（推荐 M6 终端） | 翻 `01-M3` / `01-M5` / `01-M6` / `03-§难题 1` |
| **30 min（白板推演）** | 上面 15 min 内容 + ASCII 架构图 + 设计权衡 2-3 条 + 反向提问（问面试官技术偏好） | 加 `02-§ASCII` + `02-§设计权衡` + `05-§演进路径` |

---

## 八、参考链接速查（已在仓库中的支撑材料）

| 想了解 | 翻这份 |
|---|---|
| 每个技术点的行级代码引用 | `docs-SuMon/Summary-Technology/SuSuMonitor-技术栈总结.md` (33KB) |
| 单点开发日志 | `docs-SuMon/Develop-log/2026*-*.md` (80+ 篇) |
| 单点 bug 修复 | `docs-SuMon/Bug-fix/2026*-*.md` (9 篇) |
| 单点困难 / 线上问题 | `docs-SuMon/Difficulty-log/2026*-*.md` (5 篇) |
| 项目总览与状态对齐 | 根目录 `README.md` + `项目需求与规范.md` |
| WebSocket 消息契约 | `docs-SuMon/Protocol-SuMon/websocket-protocol.md` + `message-contracts-v1.md` |
| REST OpenAPI | `docs-SuMon/OpenApi-SuMon/*.json` (5 份) |
| 部署手册 | `server-java-SuMon/deploy/DEPLOYMENT.md` + `docs-SuMon/Use-manual/` |
| 阶段交接 | `docs-SuMon/Handoff-SuMon/2026*-*.md` (2 篇) |
