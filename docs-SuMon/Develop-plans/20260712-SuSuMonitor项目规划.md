# SuSuMonitor 项目规划

**日期**: 2026-07-12  
**依据**: 根目录 `项目需求与规范.md`  
**当前状态**: 项目处于工程初始化前阶段。当前以后端 MVP-1 为优先目标，统一使用 `server-java-SuMon/` 作为 Java 后端工程目录。

## 一、已确认决策

| 决策项 | 结论 |
|--------|------|
| Java 后端目录 | 统一使用 `server-java-SuMon/` |
| 原后端目录 | `server-susumonitor/` 不再作为正式目录，后续删除前必须备份到 `C:\Backup` |
| 服务器删除策略 | 软删除 |
| 服务器新增、修改、删除权限 | 仅 `admin` 允许 |
| 服务器查看权限 | `admin` 和普通 `approved user` 都允许 |
| SSH 连接测试权限 | 仅 `admin` 允许 |
| 当前开发范围 | 只做本机开发和调试 |
| MVP-1 依赖 | 不依赖 Docker、Redis、k8s、Prometheus、Grafana、Android |
| Java 编码规范 | 遵循《阿里巴巴 Java 开发手册》 |
| Git 本机备份仓库 | `D:\develop\Git\SuSuMonitor.git` |
| Git remote 名称 | `backup` |
| opencode skill | 只保留 `.opencode/skills`，不保留 `.codex` |
| skill 语言边界 | 保留与 Go 无关内容，Java 后端遵循 Spring Boot 与阿里巴巴 Java 规范 |

## 二、规划原则

- 先完成 MVP-1 Java 后端基础闭环，再进入 Agent、实时指标、Web 前端、告警和 Web SSH 终端。
- 当前本机开发使用 MySQL 8.4，数据库地址为 `127.0.0.1:3306`，数据库名为 `susumonitor`。
- REST API 必须保持统一响应结构、统一错误码和 `X-Request-ID` 规范。
- 密码、JWT、SSH 密码、SSH 私钥、AES 密钥等敏感信息不得明文存储或输出到日志。
- SQL 查询禁止使用 `SELECT *`，需要明确字段。
- 数据库结构通过 Flyway 管理，脚本目录只承担本机初始化和辅助职责。
- 新增或修改 API 时同步维护 OpenAPI 文档和 API 调试文件。
- 开发活动完成后记录开发日志。

## 三、目标根目录结构

```text
SuSuMonitor/
├── .editorconfig
├── .gitattributes
├── .gitignore
├── .dockerignore
├── .opencode/
├── api-test/
├── docs-SuMon/
├── scripts/
├── server-java-SuMon/
├── web-vue-SuMon/
├── app-kt-SuMon/
├── 项目需求与规范.md
└── 远程Linux主机部署配置清单.md
```

| 路径 | 作用 | 当前阶段 |
|------|------|----------|
| `.editorconfig` | 统一编辑器编码、缩进、换行规则 | 当前需要 |
| `.gitattributes` | 统一 Git 换行符规则 | 当前需要 |
| `.gitignore` | 忽略本机配置、密钥、构建产物、依赖目录 | 当前需要 |
| `.dockerignore` | Docker 构建上下文忽略规则 | 增强阶段使用，当前可先建立 |
| `.opencode/` | 项目级 opencode skills 配置目录 | 当前需要 |
| `api-test/` | 本机 HTTP API 调试文件目录 | 当前需要 |
| `docs-SuMon/` | 项目正式文档目录 | 当前需要 |
| `scripts/` | 数据库初始化、本机辅助脚本目录 | 当前需要 |
| `server-java-SuMon/` | Java 后端工程目录 | 当前需要新建 |
| `web-vue-SuMon/` | Vue 3 Web 前端工程目录 | MVP-5 使用 |
| `app-kt-SuMon/` | Android App 工程目录 | 增强阶段使用 |
| `项目需求与规范.md` | 项目需求、规范、结构、配置汇总文档 | 当前持续维护 |
| `远程Linux主机部署配置清单.md` | 后续远程部署配置参考 | 后续部署阶段使用 |

## 四、旧目录处理

当前旧目录 `server-susumonitor/` 不再作为正式后端目录。

删除流程必须遵守以下步骤：

1. 确认 `server-susumonitor/` 当前内容。
2. 在 `C:\Backup` 下创建备份目录。
3. 将 `server-susumonitor/` 完整复制到 `C:\Backup`。
4. 确认备份成功。
5. 再删除原目录。

即使该目录为空，也必须先备份再删除。

## 五、MVP 阶段划分

| 阶段 | 内容 | 当前执行 |
|------|------|----------|
| MVP-1 | Java 后端新建、MySQL 建表、JWT 登录注册、用户审核、服务器 CRUD、SSH 凭据加密、SSH 连接测试 | 是 |
| MVP-2 | Agent 指标采集、5 秒采集频率、WebSocket 连接、心跳、注册校验 | 否 |
| MVP-3 | 指标接收、MySQL 存储、WebSocket 推送、历史指标查询、10 天数据清理 | 否 |
| MVP-4 | 补全、校验和整理 OpenAPI 文档 | 随接口同步，阶段性收口 |
| MVP-5 | Vue Web 前端，直接对接真实 API | 否 |
| MVP-6 | 告警规则、告警记录、WebSocket 告警推送 | 否 |
| MVP-7 | Web SSH 终端、中心服务器 SSH 代理、PTY、多会话、20 分钟超时 | 否 |
| MVP-8 | 后端启动、Agent 安装、Web 启动说明 | 否 |

## 六、MVP-1 完成标准

- 后端可在当前 Windows 主机本地启动。
- 后端监听端口为 `18080`。
- MySQL 8.4 连接成功，Flyway 可初始化基础表。
- `/api/health` 和 `/api/ready` 可调用。
- 所有 REST API 返回统一 JSON 响应结构。
- 每个 HTTP 请求生成 `request_id`，响应 header 返回 `X-Request-ID`，日志记录 request_id。
- 用户注册、登录、退出、当前用户、管理员审核流程可用。
- 首个用户自动成为 `admin/approved`。
- 后续注册用户默认为 `user/pending`，审核通过后才能登录。
- 服务器 CRUD 可用，删除为软删除。
- 服务器新增、修改、删除和 SSH 测试仅允许 `admin`。
- 普通 `approved user` 只允许查看服务器列表、详情和状态。
- SSH 密码、私钥、私钥口令使用 AES-256-GCM 加密存储。
- 查询接口不返回 SSH 敏感凭据明文。
- SSH 连接测试接口可用，并使用统一错误响应。
- 定时任务框架和 metrics 清理任务骨架存在。
- API 调试文件和 OpenAPI 文档与已实现接口同步。

## 七、Java 后端工程结构

Java 后端工程目录固定为：

```text
server-java-SuMon/
```

目标结构：

```text
server-java-SuMon/
├── pom.xml
├── README.md
├── .env.example
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── susumonitor/
│   │   │           └── server/
│   │   │               ├── SuSuMonitorServerApplication.java
│   │   │               ├── config/
│   │   │               ├── common/
│   │   │               ├── security/
│   │   │               ├── module/
│   │   │               ├── websocket/
│   │   │               ├── ssh/
│   │   │               └── scheduler/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── logback-spring.xml
│   │       └── db/
│   │           └── migration/
│   └── test/
│       └── java/
└── target/
```

| 目录或文件 | 作用 |
|------------|------|
| `pom.xml` | Maven 项目配置，管理依赖、插件、构建和测试 |
| `README.md` | Java 后端启动、配置、测试、接口调试说明 |
| `.env.example` | 本机环境变量示例，不包含真实密钥 |
| `SuSuMonitorServerApplication.java` | Spring Boot 启动入口 |
| `config/` | Web、跨域、Jackson、OpenAPI、WebSocket、加密配置 |
| `common/` | 统一响应、错误码、异常处理、分页、request_id 工具 |
| `security/` | Spring Security、JWT、登录认证、管理员权限、密码哈希 |
| `module/` | 业务模块根目录 |
| `websocket/` | WebSocket 连接、消息编解码、Agent 通道、Web 客户端通道 |
| `ssh/` | SSH 连接测试、PTY 会话、终端输入输出转发、会话关闭 |
| `scheduler/` | Agent 离线检测、metrics 清理、告警检测 |
| `application.yml` | 默认配置文件 |
| `application-local.yml` | 本机开发配置文件 |
| `logback-spring.xml` | 日志格式、日志级别、控制台和文件输出配置 |
| `db/migration/` | Flyway 数据库迁移脚本目录 |
| `target/` | Maven 构建产物目录，不提交 Git |

## 八、Java 后端技术栈

| 层级 | 技术 | 用途 |
|------|------|------|
| 开发语言 | Java 21 LTS | 后端主语言 |
| 后端框架 | Spring Boot 3.x | 应用启动、依赖注入、配置管理 |
| Web 框架 | Spring Web MVC | REST API、健康检查、就绪检查 |
| 参数校验 | Hibernate Validator | 请求参数校验 |
| 数据访问 | MyBatis-Plus | MySQL CRUD、分页查询、条件查询 |
| 数据库 | MySQL 8.4 | 当前本机开发数据库和后续主数据库 |
| 数据库连接池 | HikariCP | 数据库连接池 |
| 数据库迁移 | Flyway | 表结构初始化和版本化变更管理 |
| 认证鉴权 | Spring Security | 登录态校验、接口鉴权、管理员权限控制 |
| Token | JWT | 登录后签发 Token，接口使用 Bearer Token 鉴权 |
| 密码哈希 | BCrypt | 用户密码哈希存储 |
| SSH 凭据加密 | JDK JCA AES-GCM | SSH 密码、私钥、私钥口令加密存储 |
| WebSocket | Spring WebSocket | Agent 连接、指标推送、告警推送、SSH 终端消息 |
| SSH 客户端 | sshj | 中心服务器发起 SSH 连接、创建 PTY 会话 |
| 定时任务 | Spring Scheduling | Agent 离线检测、指标清理、告警检测 |
| JSON 序列化 | Jackson | REST API 和 WebSocket JSON 消息处理 |
| API 文档 | springdoc-openapi | 生成和维护 OpenAPI 3 文档 |
| 日志 | SLF4J + Logback | 统一日志接口和本机日志输出 |
| 构建工具 | Maven | 依赖管理、构建、测试 |
| 单元测试 | JUnit 5 | 单元测试和基础集成测试 |
| Mock 测试 | Mockito | Service、组件隔离测试 |

## 九、业务模块结构

`module/` 下按业务边界拆分：

```text
module/
├── auth/
├── admin/
├── server/
├── metrics/
├── alert/
├── dashboard/
└── sshsession/
```

| 模块 | 作用 | MVP-1 处理 |
|------|------|------------|
| `auth/` | 注册、登录、退出、当前用户信息 | 实现 |
| `admin/` | 管理员审核用户、通过、拒绝 | 实现 |
| `server/` | 服务器新增、列表、详情、更新、删除、状态查询 | 实现 |
| `metrics/` | 最新指标、历史指标、指标写入和查询 | 建表和清理骨架，接口 MVP-3 |
| `alert/` | 告警规则、告警记录、告警推送 | 建表预留，接口 MVP-6 |
| `dashboard/` | 仪表盘汇总数据 | MVP-3/MVP-5 |
| `sshsession/` | SSH 会话记录查询和关闭 | 建表预留，接口 MVP-7 |

每个业务模块内部推荐结构：

```text
模块名/
├── controller/
├── service/
├── mapper/
├── entity/
├── dto/
└── vo/
```

| 目录 | 作用 |
|------|------|
| `controller/` | REST API 入口，只处理请求参数、鉴权上下文、响应封装 |
| `service/` | 业务逻辑、事务、规则校验、跨表协作 |
| `mapper/` | MyBatis-Plus Mapper 和自定义 SQL 查询 |
| `entity/` | 数据库表映射实体 |
| `dto/` | 请求参数对象 |
| `vo/` | 响应对象 |

## 十、权限设计

| 接口 | admin | approved user | 未登录 |
|------|-------|---------------|--------|
| `GET /api/servers` | 允许 | 允许 | 拒绝 |
| `GET /api/servers/{id}` | 允许 | 允许 | 拒绝 |
| `GET /api/servers/{id}/status` | 允许 | 允许 | 拒绝 |
| `POST /api/servers` | 允许 | 拒绝 | 拒绝 |
| `PUT /api/servers/{id}` | 允许 | 拒绝 | 拒绝 |
| `DELETE /api/servers/{id}` | 允许 | 拒绝 | 拒绝 |
| `POST /api/servers/{id}/ssh/test` | 允许 | 拒绝 | 拒绝 |

其他权限规则：

- `/api/health` 公开。
- `/api/ready` MVP-1 阶段公开，用于本机调试。
- `/api/auth/register` 公开。
- `/api/auth/login` 公开。
- `/api/auth/logout` 需要登录。
- `/api/auth/me` 需要登录。
- `/api/admin/**` 仅 `admin` 允许。

## 十一、服务器软删除设计

`servers` 表采用软删除，不物理删除记录。

建议字段：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `deleted` | tinyint / boolean | `0` | `0` 未删除，`1` 已删除 |
| `deleted_at` | datetime | `NULL` | 删除时间 |
| `delete_token` | varchar(64) | `ACTIVE` | 用于支持同一 host 软删除后重新添加 |

查询规则：

| 场景 | 规则 |
|------|------|
| 服务器列表 | 只查询 `deleted = 0` |
| 服务器详情 | 只允许查询 `deleted = 0` |
| 更新服务器 | 只允许更新 `deleted = 0` |
| 删除服务器 | 设置 `deleted = 1`、`deleted_at = now()`、`delete_token = UUID` |
| Agent 关联 | 默认只关联 `deleted = 0` 的服务器 |

唯一索引建议：

```text
uk_servers_host_delete_token(host, delete_token)
```

新增服务器时：

```text
deleted = 0
delete_token = ACTIVE
```

软删除服务器时：

```text
deleted = 1
```

## 十二、数据库设计

MVP-1 需要维护基础表：

```text
users
servers
metrics
commands
alert_rules
alert_records
ssh_sessions
```

| 表 | MVP-1 作用 |
|----|------------|
| `users` | 用户注册、登录、审核 |
| `servers` | 服务器管理、Agent 预留字段、SSH 凭据密文 |
| `metrics` | 建表预留，MVP-3 写入和查询 |
| `commands` | 命令记录，增强阶段使用 |
| `alert_rules` | 告警规则，MVP-6 使用 |
| `alert_records` | 告警记录，MVP-6 使用 |
| `ssh_sessions` | SSH 会话生命周期记录，MVP-7 使用 |

关键索引：

| 索引 | 用途 |
|------|------|
| `uk_users_username` | 用户名唯一 |
| `uk_servers_host_delete_token` | 活跃服务器 host 唯一，支持软删除后重建 |
| `uk_servers_agent_id` | Agent ID 唯一 |
| `idx_metrics_collected_at` | 指标清理 |
| `idx_metrics_server_time` | 历史指标查询 |

Flyway 迁移目录：

```text
server-java-SuMon/src/main/resources/db/migration/
```

建议迁移文件：

```text
V1__create_users_table.sql
V2__create_servers_table.sql
V3__create_metrics_table.sql
V4__create_commands_table.sql
V5__create_alert_tables.sql
V6__create_ssh_sessions_table.sql
```

## 十三、配置设计

配置文件：

```text
server-java-SuMon/src/main/resources/application.yml
server-java-SuMon/src/main/resources/application-local.yml
server-java-SuMon/.env.example
```

真实密钥只放本机环境或本机配置文件，不提交到 Git。

| 分类 | 配置项 |
|------|--------|
| 应用 | `APP_ENV`、`APP_NAME`、`SERVER_PORT` |
| 数据库 | `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD`、`DB_CHARSET`、`DB_TIMEZONE` |
| JWT | `JWT_SECRET`、`JWT_EXPIRE_HOURS` |
| SSH 加密 | `AES_GCM_KEY`、`SSH_CONNECT_TIMEOUT_SECONDS`、`SSH_IDLE_TIMEOUT_MINUTES` |
| Agent | `AGENT_REGISTER_KEY`、`AGENT_HEARTBEAT_TIMEOUT_SECONDS`、`AGENT_STATUS_SCAN_SECONDS` |
| 调度 | `METRICS_RETENTION_DAYS`、`METRICS_CLEANUP_CRON`、`ALERT_CHECK_SECONDS` |

安全要求：

- `JWT_SECRET` 不硬编码、不提交。
- `AES_GCM_KEY` 不硬编码、不提交。
- `AGENT_REGISTER_KEY` 不硬编码、不提交。
- 日志不输出密码、Token、SSH 私钥、SSH 密码、AES 密钥。

## 十四、REST API 设计

统一成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

统一分页响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [],
    "total": 100,
    "page": 1,
    "page_size": 20
  }
}
```

统一错误响应：

```json
{
  "code": 40000,
  "message": "bad request"
}
```

MVP-1 接口范围：

| 模块 | 接口 | 权限 |
|------|------|------|
| 系统 | `GET /api/health` | 公开 |
| 系统 | `GET /api/ready` | 公开 |
| 认证 | `POST /api/auth/register` | 公开 |
| 认证 | `POST /api/auth/login` | 公开 |
| 认证 | `POST /api/auth/logout` | 登录用户 |
| 认证 | `GET /api/auth/me` | 登录用户 |
| 管理员 | `GET /api/admin/users/pending` | admin |
| 管理员 | `PUT /api/admin/users/{id}/approve` | admin |
| 管理员 | `PUT /api/admin/users/{id}/reject` | admin |
| 服务器 | `GET /api/servers` | approved user / admin |
| 服务器 | `POST /api/servers` | admin |
| 服务器 | `GET /api/servers/{id}` | approved user / admin |
| 服务器 | `PUT /api/servers/{id}` | admin |
| 服务器 | `DELETE /api/servers/{id}` | admin |
| 服务器 | `GET /api/servers/{id}/status` | approved user / admin |
| SSH | `POST /api/servers/{id}/ssh/test` | admin |

## 十五、认证与用户审核设计

| 项 | 规则 |
|----|------|
| username | 3 到 50 位，允许字母、数字、下划线 |
| password | 8 到 64 位 |
| password_hash | 使用 BCrypt 存储 |
| JWT 有效期 | 默认 24 小时，通过配置项调整 |
| 首个用户 | 自动成为 `admin/approved` |
| 后续注册用户 | 默认为 `user/pending` |
| 登录限制 | 只有 `approved` 用户可以登录 |
| pending/rejected 用户 | 登录返回 403 |
| logout | MVP 阶段服务端无状态退出，前端删除 Token |

角色：

```text
admin
user
```

审核状态：

```text
pending
approved
rejected
```

## 十六、SSH 凭据与连接测试设计

服务器字段：

| 字段 | 存储方式 |
|------|----------|
| `name` | 明文 |
| `host` | 明文 |
| `description` | 明文 |
| `ssh_host` | 明文 |
| `ssh_port` | 明文 |
| `ssh_user` | 明文 |
| `ssh_auth_type` | 明文 |
| `ssh_password` | 入参字段，存储为 AES-GCM 密文 |
| `ssh_private_key` | 入参字段，存储为 AES-GCM 密文 |
| `ssh_private_key_passphrase` | 入参字段，存储为 AES-GCM 密文 |

数据库密文字段：

```text
ssh_password_encrypted
ssh_private_key_encrypted
ssh_private_key_passphrase_encrypted
```

认证方式规则：

| `ssh_auth_type` | 必填字段 |
|-----------------|----------|
| `password` | `ssh_password` |
| `private_key` | `ssh_private_key` |

AES-GCM 规则：

| 项 | 规则 |
|----|------|
| 算法 | AES-256-GCM |
| IV | 每次加密随机生成 |
| 密钥来源 | `AES_GCM_KEY` |
| 存储格式 | 建议 `base64(iv):base64(ciphertext)` |
| 日志 | 不输出明文、密文、密钥 |

SSH 连接测试接口：

```text
POST /api/servers/{id}/ssh/test
```

流程：

1. 校验用户为 `admin`。
2. 查询未软删除的服务器记录。
3. 根据 `ssh_auth_type` 解密对应凭据。
4. 使用 sshj 建立 SSH 连接。
5. 成功后立即关闭连接。
6. 失败返回 `50002 ssh connection failed`。
7. 日志只记录 serverId、sshHost、错误类型，不记录敏感内容。

## 十七、定时任务设计

MVP-1 只做定时任务框架和 metrics 清理任务骨架。

| 任务 | MVP-1 处理 |
|------|------------|
| metrics 清理 | 建立任务骨架和配置项 |
| Agent 离线检测 | 预留，MVP-2/MVP-3 完善 |
| 告警检测 | 预留，MVP-6 完善 |

配置项：

```text
METRICS_RETENTION_DAYS=10
METRICS_CLEANUP_CRON=0 0 3 * * ?
ALERT_CHECK_SECONDS=10
AGENT_STATUS_SCAN_SECONDS=30
```

## 十八、文档目录设计

```text
docs-SuMon/
├── Develop-plans/
├── Develop-log/
├── OpenApi-SuMon/
├── Protocol-SuMon/
└── 本机开发环境配置.md
```

| 路径 | 作用 |
|------|------|
| `docs-SuMon/Develop-plans/` | 开发计划目录 |
| `docs-SuMon/Develop-log/` | 开发日志目录 |
| `docs-SuMon/OpenApi-SuMon/` | REST API OpenAPI 3.0 JSON 文档目录 |
| `docs-SuMon/Protocol-SuMon/` | WebSocket、Agent、SSH 终端协议文档目录 |
| `docs-SuMon/本机开发环境配置.md` | 当前本机开发环境、MySQL、后端启动和验证说明 |

文档维护规则：

- 新增或修改 API 时，同步更新 OpenAPI 文档。
- 新增或修改 WebSocket/Agent 协议时，同步更新协议文档。
- 开发活动完成后记录开发日志。
- 文档中的服务器地址示例统一使用 `SERVER_IP_OR_DOMAIN`，不使用真实 IP 或内网 IP。

## 十九、API 调试目录设计

```text
api-test/
└── susumonitor.http
```

调试变量：

```http
@baseUrl = http://localhost:18080
@token = REPLACE_WITH_JWT_TOKEN
@serverId = 1
```

请求分组：

| 分组 | 请求 |
|------|------|
| System | health、ready |
| Auth | register、login、me、logout |
| Admin | pending、approve、reject |
| Servers | list、create、detail、update、delete、status |
| SSH | test |
| Metrics | MVP-3 预留 |
| Alerts | MVP-6 预留 |

## 二十、脚本目录设计

```text
scripts/
└── local-mysql-init.sql
```

`local-mysql-init.sql` 职责：

| 内容 | 要求 |
|------|------|
| 创建数据库 | `susumonitor` |
| 字符集 | `utf8mb4` |
| 排序规则 | `utf8mb4_unicode_ci` |
| 创建用户 | `susumonitor` |
| 登录来源 | `localhost` 和 `127.0.0.1` |
| 授权 | 授予 `susumonitor` 数据库权限 |

说明：业务表结构优先由 Flyway 管理，避免脚本和迁移重复建表。

## 二十一、根配置文件设计

需要建立：

```text
.editorconfig
.gitattributes
.gitignore
.dockerignore
```

`.editorconfig` 规则：

| 文件类型 | 缩进 |
|----------|------|
| 默认 | 2 空格 |
| JSON/YAML | 2 空格 |
| Kotlin | 4 空格 |
| SQL | 4 空格 |
| Markdown | 保留行尾空格以兼容换行 |

`.gitignore` 应忽略：

| 类别 | 内容 |
|------|------|
| 系统文件 | `.DS_Store`、`Thumbs.db`、`desktop.ini` |
| IDE | `.idea/`、`.vscode/` |
| 环境和密钥 | `.env`、`.env.*`、证书、私钥、`secrets/` |
| 日志 | `*.log`、`logs/` |
| Java 构建产物 | `target/` |
| 前端依赖和产物 | `node_modules/`、`dist/`、`.vite/` |
| Android 产物 | `.gradle/`、`local.properties`、`app/build/`、`captures/` |
| Docker/k8s 本地产物 | `*.tar`、`*.tgz`、`tmp/` |

`.gitattributes` 规则：

```text
* text=auto eol=lf
*.bat text eol=crlf
*.cmd text eol=crlf
*.ps1 text eol=crlf
```

`.dockerignore` 应排除 Git、IDE、本机环境密钥、依赖、构建产物、日志和临时目录。

## 二十二、实施顺序

### 阶段 0：结构基线

目标：让项目结构和新文档一致。

| 任务 | 验收 |
|------|------|
| 备份并删除旧目录 `server-susumonitor/` | `C:\Backup` 下存在备份，项目根目录不再有旧目录 |
| 新建 `server-java-SuMon/` | Java 后端正式目录存在 |
| 补齐 `scripts/` 和文档子目录 | 目录结构符合文档 |

### 阶段 1：根配置和辅助文件初始化

目标：建立项目工程外壳。

| 任务 | 验收 |
|------|------|
| 新建 `.editorconfig` | 编码、缩进、换行规则明确 |
| 新建 `.gitattributes` | Git 换行规则明确 |
| 新建 `.gitignore` | 密钥和构建产物不会提交 |
| 新建 `.dockerignore` | 容器化阶段预留 |
| 新建 `scripts/local-mysql-init.sql` | 可初始化本机数据库和用户 |
| 新建 `api-test/susumonitor.http` | 包含 MVP-1 调试请求框架 |

### 阶段 2：Java 后端工程初始化

目标：后端能启动、能测试。

| 任务 | 验收 |
|------|------|
| 初始化 Maven 工程 | `pom.xml` 存在 |
| 添加 Spring Boot 入口 | 应用可启动 |
| 添加配置文件 | 端口、数据库、JWT、AES、Agent 配置项存在 |
| 添加 logback 配置 | 日志格式明确 |
| 添加 Flyway 目录 | 迁移目录存在 |
| 添加基础测试 | `mvn test` 可运行 |

### 阶段 3：横切基础能力

目标：所有后续接口有统一基础。

| 任务 | 验收 |
|------|------|
| 统一响应对象 | 成功、错误、分页结构统一 |
| 错误码枚举 | 覆盖文档错误码 |
| 全局异常处理 | 参数、认证、权限、资源、数据库、SSH 错误统一 |
| request_id 过滤器 | 响应头返回 `X-Request-ID` |
| 日志 MDC | 日志包含 request_id |
| health | `GET /api/health` 可用 |
| ready | `GET /api/ready` 检查数据库 |

### 阶段 4：数据库迁移

目标：基础表结构落地。

| 任务 | 验收 |
|------|------|
| 创建 `users` 表 | 用户注册登录可用 |
| 创建 `servers` 表 | 服务器 CRUD 可用，支持软删除 |
| 创建 `metrics` 表 | MVP-3 可复用 |
| 创建 `commands` 表 | 增强阶段可复用 |
| 创建告警表 | MVP-6 可复用 |
| 创建 SSH 会话表 | MVP-7 可复用 |
| 添加索引 | 文档要求索引存在 |

### 阶段 5：认证与审核

目标：用户闭环跑通。

| 任务 | 验收 |
|------|------|
| 注册 | 首个用户自动 admin/approved |
| 登录 | approved 用户可获取 JWT |
| logout | 无状态退出 |
| me | JWT 可查询当前用户 |
| pending list | admin 可查看待审核 |
| approve | admin 可审核通过 |
| reject | admin 可拒绝 |

### 阶段 6：服务器管理

目标：服务器 CRUD 闭环。

| 任务 | 验收 |
|------|------|
| 新增服务器 | 仅 admin，字段校验完整 |
| 查询列表 | admin 和 approved user 可查，支持分页、keyword、排序 |
| 查询详情 | admin 和 approved user 可查，不返回 SSH 敏感明文 |
| 更新服务器 | 仅 admin，支持 SSH 凭据更新 |
| 删除服务器 | 仅 admin，执行软删除 |
| 状态查询 | admin 和 approved user 可查，Agent 状态 MVP-2 后完善 |

### 阶段 7：SSH 凭据加密与连接测试

目标：服务器 SSH 能力闭环。

| 任务 | 验收 |
|------|------|
| AES-GCM 工具 | 可加解密，密钥来自配置 |
| 凭据加密存储 | 数据库无明文 |
| SSH test | 仅 admin，支持 password/private_key |
| 错误处理 | 失败返回 `50002` |
| 日志脱敏 | 不输出敏感字段 |

### 阶段 8：MVP-1 收口

目标：MVP-1 可验收。

| 任务 | 验收 |
|------|------|
| metrics 清理任务骨架 | 定时任务可触发 |
| API 调试文件补齐 | MVP-1 接口可调试 |
| OpenAPI 文档同步 | 与实现一致 |
| 后端 README | 启动、配置、测试说明完整 |
| 本机开发环境文档 | MySQL、启动、验证说明完整 |
| 开发日志 | 记录本轮变更 |

## 二十三、验证命令和验收流程

后续实现完成后至少验证：

```bash
mvn test
```

```bash
curl http://localhost:18080/api/health
curl http://localhost:18080/api/ready
```

认证流程：

```text
register first admin
login admin
GET /api/auth/me
register second user
login second user -> 403
admin approve second user
login second user -> success
```

服务器流程：

```text
admin create server
admin list servers
approved user list servers
approved user create server -> 403
approved user update server -> 403
approved user delete server -> 403
admin update server
admin ssh test
admin delete server -> soft delete
deleted server not shown in list
```

数据库检查：

```text
users.password_hash 不能是明文
servers.ssh_password_encrypted 不能是明文
servers.ssh_private_key_encrypted 不能是明文
servers.deleted = 1 表示软删除
servers.delete_token 支持软删除后同 host 重建
```

## 二十四、风险与处理

| 风险或问题 | 影响 | 处理 |
|------------|------|------|
| 当前存在旧目录 `server-susumonitor/` | 可能混淆后端目录 | 备份后删除，统一使用 `server-java-SuMon/` |
| 本机 MySQL 用户和权限未确认 | `/api/ready` 和 Flyway 可能失败 | 先执行或检查 `scripts/local-mysql-init.sql` |
| 本机 SSH 服务未确认 | SSH test 可能无法成功验证 | 先完成接口和错误处理，真实成功测试依赖可用 SSH 服务 |
| AES-GCM 密钥来源不当 | 凭据加密不安全 | 只从配置或环境变量注入，不硬编码 |
| 普通用户误操作服务器 | 影响服务器资产安全 | 增删改删和 SSH test 仅 admin |
| 软删除唯一索引设计不当 | 同 host 删除后无法重建 | 使用 `delete_token` 参与唯一索引 |

## 二十五、下一步执行建议

建议按以下顺序开始实施：

1. 备份并删除旧目录 `server-susumonitor/`。
2. 初始化根配置文件。
3. 初始化 `scripts/`、`api-test/`、`docs-SuMon/` 子目录。
4. 新建 `server-java-SuMon/` Maven + Spring Boot 3 + Java 21 工程。
5. 实现 `/api/health`、`/api/ready`、统一响应、错误码、request_id 和日志。
6. 实现 Flyway 表结构。
7. 实现认证、审核、服务器 CRUD、SSH 凭据加密和 SSH 测试。
8. 同步 OpenAPI、HTTP 调试文件、README、本机开发环境文档和开发日志。

## 二十六、AI 运维中枢规划

### 26.1 能力定位

后续在中心主机接入能力较强的 AI API Key，将 SuSuMonitor 扩展为带 AI 决策能力的多主机监控与自动运维平台。

目标能力：

- 持续收集本机和其他主机的硬件、系统、服务状态。
- 由 AI 综合判断所有主机的健康状态、异常风险和可能原因。
- AI 生成结构化处置建议。
- 低风险操作可按配置自动执行。
- 中高风险操作推送到 Android 客户端请求人工确认。
- 所有 AI 建议、审批、执行和结果都记录审计日志。

AI 能力不纳入 MVP-1 主线。MVP-1 仍优先完成 Java 后端基础、用户权限、服务器管理、SSH 凭据安全存储和基础接口。

### 26.2 总体架构

```text
Web 前端 / Android 客户端
        ↓
server-java-SuMon 中心服务
        ↓
AI 运维决策模块 aiops
        ↓
Agent / SSH / 命令执行通道
        ↓
本机和其他 Linux 主机
```

新增后端模块建议：

```text
server-java-SuMon/src/main/java/com/susumonitor/server/module/aiops/
├── controller/
├── service/
├── mapper/
├── entity/
├── dto/
└── vo/
```

可配套新增通知模块：

```text
server-java-SuMon/src/main/java/com/susumonitor/server/notification/
├── NotificationService.java
├── WebSocketNotificationSender.java
└── PushNotificationSender.java
```

第一版通知可优先使用 WebSocket，后续再接入 FCM 或国内厂商推送。

### 26.3 AI API Key 管理

AI API Key 只保存在中心主机，不下发给 Agent，不返回给 Web 或 Android。

建议配置项：

```text
AI_PROVIDER=openai
AI_API_KEY=本机环境变量
AI_MODEL=强模型名称
AI_REQUEST_TIMEOUT_SECONDS=60
AI_MAX_TOKENS=4096
AI_OPS_ENABLED=true
```

安全要求：

- AI API Key 不写入 Git。
- AI API Key 不写入日志。
- AI API Key 不返回给前端或 Android。
- AI API Key 不传给其他主机 Agent。
- AI 调用日志只保存模型名、请求摘要、结果摘要、耗时和状态，不保存密钥。

### 26.4 AI 分析流程

```text
定时任务或用户手动触发
        ↓
拉取最近 N 分钟所有主机指标、告警、Agent 状态和服务状态
        ↓
聚合为 AI 分析上下文
        ↓
调用 AI 模型
        ↓
AI 返回结构化 JSON
        ↓
后端校验 JSON、动作白名单、风险等级和权限策略
        ↓
生成 AI 分析报告和建议操作
        ↓
按风险等级自动执行、等待审批或直接拒绝
```

AI 返回结果必须是结构化 JSON，不能直接执行自由文本命令。

示例结构：

```json
{
  "summary": "当前整体状态",
  "overall_status": "healthy|warning|critical",
  "hosts": [
    {
      "server_id": 1,
      "status": "warning",
      "problems": ["CPU 使用率持续过高"],
      "possible_causes": ["某进程占用异常"],
      "recommended_actions": [
        {
          "action": "inspect_top_processes",
          "risk_level": "L1",
          "need_approval": false,
          "reason": "需要查看进程占用"
        }
      ]
    }
  ]
}
```

后端必须校验：

- JSON 格式是否合法。
- `action` 是否在动作白名单中。
- `risk_level` 是否符合系统策略。
- 当前系统是否允许 AI 操作。
- 当前主机是否允许 AI 操作。
- 是否需要 Android 或 Web 人工确认。

### 26.5 风险等级与执行策略

| 风险等级 | 类型 | 示例 | 处理方式 |
|----------|------|------|----------|
| L1 | 只读分析 | 查看指标、查看进程列表、查看磁盘使用 | 自动执行 |
| L2 | 低风险操作 | 重启 Agent、刷新状态、清理临时缓存 | 可配置自动执行 |
| L3 | 中风险操作 | 重启业务服务、清理较大日志、修改非核心配置 | 推送 Android 请求人工确认 |
| L4 | 高风险操作 | 重启主机、关机、修改防火墙、修改 SSH 配置 | 推送 Android 请求人工确认，默认更严格 |
| L5 | 禁止操作 | 删除系统目录、格式化磁盘、清空数据库、关闭安全组件 | 永久禁止，不进入确认流程 |

默认策略：

- L1 自动执行。
- L2 默认不自动执行，可通过配置开启。
- L3/L4 必须人工确认。
- L5 直接拒绝。

### 26.6 动作白名单

AI 不允许直接生成 shell 命令并执行，只允许返回系统预定义动作。

| action | 风险等级 | 是否需要 Android 确认 |
|--------|----------|------------------------|
| `inspect_top_processes` | L1 | 否 |
| `inspect_disk_usage` | L1 | 否 |
| `inspect_memory_usage` | L1 | 否 |
| `inspect_service_status` | L1 | 否 |
| `restart_agent` | L2 | 可配置 |
| `cleanup_temp_files` | L2 | 可配置 |
| `restart_service` | L3 | 是 |
| `cleanup_large_logs` | L3 | 是 |
| `reload_nginx` | L3 | 是 |
| `reboot_server` | L4 | 是 |
| `shutdown_server` | L4 | 是 |
| `change_firewall_rule` | L4 | 是 |
| `modify_ssh_config` | L4 | 是 |
| `delete_directory` | L5 | 禁止 |
| `format_disk` | L5 | 禁止 |
| `drop_database` | L5 | 禁止 |

错误做法：

```text
AI 返回 rm -rf /xxx，然后系统执行
```

正确做法：

```text
AI 返回 action=cleanup_temp_files
后端检查 action 白名单
后端检查风险等级
后端检查审批策略
后端调用 Agent 或 SSH 通道执行预定义逻辑
```

### 26.7 Android 中高风险审批流程

中高风险操作需要发送到 Android 客户端请求人工确认。

流程：

```text
AI 发现问题并生成建议操作
        ↓
后端校验为 L3/L4 操作
        ↓
创建 ai_action_record 和审批记录
        ↓
推送通知到 Android 客户端
        ↓
用户打开操作详情页
        ↓
查看风险等级、主机、问题、AI 原因、影响范围和超时时间
        ↓
用户同意或拒绝
        ↓
后端再次校验用户身份、admin 权限、操作状态和过期时间
        ↓
执行或取消操作
        ↓
记录审批日志和执行结果
```

Android 通知内容建议：

| 内容 | 示例 |
|------|------|
| 风险等级 | L3 中风险 |
| 主机 | `SERVER_IP_OR_DOMAIN` / 主机名称 |
| 问题 | 内存使用率持续超过 90% |
| AI 建议 | 重启指定服务 |
| 操作类型 | `restart_service` |
| 影响范围 | 可能导致服务短暂不可用 |
| 超时时间 | 10 分钟内未确认则自动取消 |
| 操作按钮 | 同意 / 拒绝 |

Android 确认不能只依赖通知按钮直接执行，必须进入详情页后确认。

对于 L4 高风险操作，建议增加二次确认，例如输入主机名或输入 `CONFIRM`。

### 26.8 审批状态机

正常流程：

```text
created
  ↓
pending_approval
  ↓ approve
approved
  ↓
running
  ↓
success / failed
```

拒绝流程：

```text
pending_approval
  ↓ reject
rejected
```

超时流程：

```text
pending_approval
  ↓ timeout
expired
```

取消流程：

```text
pending_approval
  ↓ cancel
canceled
```

执行前必须再次校验：

- action 仍在白名单。
- 风险等级未被篡改。
- 审批人仍是 `admin`。
- 主机仍存在且未软删除。
- 操作未过期。
- 操作没有被重复执行。
- 当前系统未开启全局只读模式。
- 目标主机未禁用 AI 操作。

### 26.9 AI 运维接口规划

后续建议新增接口：

| 接口 | 作用 | 权限 |
|------|------|------|
| `POST /api/aiops/analyze` | 手动触发 AI 分析 | admin |
| `GET /api/aiops/reports` | 查询 AI 分析报告 | approved user / admin |
| `GET /api/aiops/reports/{id}` | 查询 AI 分析报告详情 | approved user / admin |
| `GET /api/aiops/actions/pending` | 查询待确认操作 | admin |
| `GET /api/aiops/actions/{id}` | 查询操作详情 | admin |
| `POST /api/aiops/actions/{id}/approve` | 同意执行 | admin |
| `POST /api/aiops/actions/{id}/reject` | 拒绝执行 | admin |
| `POST /api/aiops/actions/{id}/cancel` | 取消待执行操作 | admin |
| `GET /api/aiops/actions/history` | 查询历史操作 | admin |

Android 使用同一套 REST API，不单独设计另一套权限系统。

### 26.10 AI 运维数据表规划

后续建议新增表：

```text
ai_analysis_reports
ai_action_records
ai_action_approvals
ai_notification_records
ai_model_call_logs
```

`ai_analysis_reports` 用于保存 AI 分析报告。

| 字段 | 说明 |
|------|------|
| `id` | 主键 |
| `overall_status` | healthy / warning / critical |
| `summary` | AI 总结 |
| `report_json` | 结构化分析结果 |
| `created_at` | 创建时间 |

`ai_action_records` 用于保存 AI 建议或执行的操作。

| 字段 | 说明 |
|------|------|
| `id` | 操作记录 ID |
| `server_id` | 主机 ID |
| `action_type` | 动作类型 |
| `risk_level` | L1/L2/L3/L4/L5 |
| `status` | pending_approval / approved / rejected / running / success / failed / canceled / expired |
| `reason` | AI 判断原因 |
| `impact` | 影响说明 |
| `requested_by` | ai / user |
| `approved_by` | 审批人 |
| `approved_at` | 审批时间 |
| `expires_at` | 确认超时时间 |
| `executed_at` | 执行时间 |
| `result_summary` | 执行结果摘要 |
| `created_at` | 创建时间 |

`ai_action_approvals` 用于保存审批记录。

| 字段 | 说明 |
|------|------|
| `id` | 审批记录 ID |
| `action_record_id` | 关联操作 |
| `approver_user_id` | 审批人 |
| `decision` | approved / rejected |
| `comment` | 审批备注 |
| `client_type` | android / web |
| `created_at` | 审批时间 |

`ai_notification_records` 用于保存通知记录。

| 字段 | 说明 |
|------|------|
| `id` | 通知记录 ID |
| `action_record_id` | 关联操作 |
| `user_id` | 通知接收人 |
| `channel` | android_ws / web_ws / push |
| `status` | pending / sent / failed / read |
| `sent_at` | 发送时间 |
| `read_at` | 阅读时间 |

`ai_model_call_logs` 用于保存 AI 调用记录，但不得保存 API Key。

### 26.11 Android 客户端能力规划

Android App 后续新增 AI 运维能力：

```text
AI 运维
├── 待确认操作
├── 操作详情
├── 历史记录
└── 通知设置
```

页面能力：

| 页面 | 能力 |
|------|------|
| 待确认操作 | 查看所有等待审批的 L3/L4 操作 |
| 操作详情 | 查看主机、问题、AI 原因、风险、影响、超时和执行动作 |
| 历史记录 | 查看已批准、已拒绝、已执行、已失败、已超时的操作 |
| 通知设置 | 配置是否接收 AI 审批通知 |

Android 安全要求：

- 用户必须登录。
- Token 未过期。
- 用户必须是 `admin` 才能审批。
- 点击通知后进入详情页，不能直接执行。
- 审批时后端再次校验权限和操作状态。
- L4 操作需要二次确认。

### 26.12 AI 运维配置项规划

建议新增配置项：

```text
AI_OPS_ENABLED=true
AI_AUTO_ACTION_ENABLED=false
AI_MAX_AUTO_RISK_LEVEL=L2
AI_REQUIRE_APPROVAL_RISK_LEVEL=L3
AI_ACTION_APPROVAL_TIMEOUT_MINUTES=10
AI_ANDROID_APPROVAL_ENABLED=true
AI_WEB_APPROVAL_ENABLED=true
AI_GLOBAL_READ_ONLY=false
```

| 配置项 | 说明 |
|--------|------|
| `AI_OPS_ENABLED` | 是否启用 AI 运维 |
| `AI_AUTO_ACTION_ENABLED` | 是否允许自动执行低风险操作 |
| `AI_MAX_AUTO_RISK_LEVEL` | 最高自动执行风险等级 |
| `AI_REQUIRE_APPROVAL_RISK_LEVEL` | 从哪个等级开始必须审批 |
| `AI_ACTION_APPROVAL_TIMEOUT_MINUTES` | 审批超时时间 |
| `AI_ANDROID_APPROVAL_ENABLED` | 是否启用 Android 审批 |
| `AI_WEB_APPROVAL_ENABLED` | 是否启用 Web 审批 |
| `AI_GLOBAL_READ_ONLY` | 全局只读模式 |

默认建议：

```text
AI_AUTO_ACTION_ENABLED=false
AI_REQUIRE_APPROVAL_RISK_LEVEL=L3
AI_ANDROID_APPROVAL_ENABLED=true
AI_WEB_APPROVAL_ENABLED=true
AI_GLOBAL_READ_ONLY=false
```

### 26.13 AI 阶段路线

AI 能力建议作为 MVP 后续增强阶段，不影响 MVP-1 实施优先级。

| 阶段 | 目标 |
|------|------|
| AI-1 | AI 只读健康分析 |
| AI-2 | AI 告警解释和处置建议 |
| AI-3 | AI 低风险动作白名单自动执行 |
| AI-4 | 中高风险操作推送 Android 请求人工确认 |
| AI-5 | Web 和 Android 双端审批、操作结果追踪 |
| AI-6 | 故障复盘、趋势预测、容量规划 |

当前结论：AI 运维中枢是后续重要方向，但不改变 MVP-1 的主线。MVP-1 仍先建设权限、服务器管理、SSH 凭据安全、基础表结构和接口规范，为后续 AI 操作审批和审计打基础。

## 二十七、Git、环境配置与 opencode skill 规划

### 27.1 Git 本机备份仓库

当前项目使用本机 bare Git 仓库作为备份远程仓库。

| 项 | 配置 |
|----|------|
| 工作区 | `C:\Users\genhaosan\Desktop\SuSuMonitor(Jvav)` |
| 本机 bare 仓库 | `D:\develop\Git\SuSuMonitor.git` |
| remote 名称 | `backup` |
| 默认分支 | `main` |

使用原则：

- `D:\develop\Git\SuSuMonitor.git` 是 bare 仓库，只用于 `git push` 备份，不直接编辑源码。
- 日常开发仍在 `C:\Users\genhaosan\Desktop\SuSuMonitor(Jvav)` 工作区进行。
- 首次提交和推送需要明确确认后再执行。
- 提交信息遵循 Conventional Commits，例如 `chore(project): 初始化 SuSuMonitor Java 项目规划`。

### 27.2 Git 忽略规则

项目根目录维护 `.gitignore`，用于忽略本机配置、密钥、构建产物和工具依赖。

必须忽略：

```text
.env
.env.*
*.pem
*.key
*.crt
secrets/
target/
node_modules/
dist/
.gradle/
local.properties
logs/
tmp/
.opencode/node_modules/
.opencode/package.json
.opencode/package-lock.json
.opencode/bun.lock
.codex/
```

说明：

- `.env.example` 可以提交，用于说明配置项。
- `.codex/` 不作为本项目配置体系，统一忽略。
- `.opencode/skills/` 需要保留和提交。
- `.opencode` 下 package 相关文件属于本地工具依赖，不作为项目正式配置。

### 27.3 环境配置策略

Java 后端环境配置以后续 `server-java-SuMon/` 工程为准。

建议文件：

```text
server-java-SuMon/.env.example
server-java-SuMon/src/main/resources/application.yml
server-java-SuMon/src/main/resources/application-local.yml
```

配置原则：

- 真实密钥只放本机环境变量或本机 `.env`，不提交 Git。
- `.env.example` 只写示例值和占位符，不写真实密钥。
- `JWT_SECRET`、`AES_GCM_KEY`、`AGENT_REGISTER_KEY` 不允许硬编码。
- 启动时应对关键密钥为空进行校验，避免使用空密钥运行。
- 日志中不得输出数据库密码、JWT、AES 密钥、Agent 注册密钥、SSH 密码、SSH 私钥。

### 27.4 opencode skill 保留策略

项目只保留 `.opencode/skills`，不保留 `.codex`。

当前保留的 skill：

```text
.opencode/skills/
├── docker-standards/
├── git-conventions/
├── kotlin-coding/
├── mysql-standards/
├── susumonitor-docs/
└── vue3-coding/
```

| skill | 处理 |
|-------|------|
| `docker-standards` | 保留，容器化阶段使用 |
| `git-conventions` | 保留，Git 提交、分支和提交粒度规范 |
| `kotlin-coding` | 保留，Android 阶段使用 |
| `mysql-standards` | 保留，数据库设计和 SQL 编写使用 |
| `vue3-coding` | 保留，Web 前端阶段使用 |
| `susumonitor-docs` | 保留文档规范，移除 Go 编码规范，改为 Java/Spring Boot 与项目文档规范 |

### 27.5 Java 编码规范

所有 Java 代码必须遵循《阿里巴巴 Java 开发手册》和项目内 `susumonitor-docs` skill 中的 Java 后端规范。

核心约束：

- 类名使用 UpperCamelCase。
- 方法名、变量名使用 lowerCamelCase。
- 常量使用 UPPER_SNAKE_CASE。
- 布尔变量不使用 `is` 前缀。
- Controller 不写业务逻辑，业务规则放在 Service。
- DTO、VO、Entity 分离。
- SQL 禁止 `SELECT *`。
- 异常捕获后必须处理，不允许吞异常。
- 日志使用 SLF4J 门面和 `{}` 占位符，不使用字符串拼接输出变量。
- 敏感信息不写日志、不返回前端、不提交 Git。

### 27.6 opencode 生效说明

`.opencode/skills` 属于 opencode 配置时加载内容。修改 skill 后，当前运行中的 opencode 会继续使用已加载版本。

后续如需让新的 skill 内容完全生效，需要退出并重启 opencode。
