---
name: susumonitor-docs
description: SuSuMonitor 文档与 Java 后端规范。当在 SuSuMonitor 项目目录下编写开发计划、开发日志、OpenAPI、协议文档或 Java/Spring Boot 后端代码时使用。
---

# SuSuMonitor 文档规范

## 文档目录结构

项目根目录下的 `docs-SuMon/` 是文档根目录，包含四个子目录：

```
docs-SuMon/
├── Develop-plans/    # 开发计划 (.md 文件)
├── Develop-log/      # 开发日志 (.md 文件)
├── OpenApi-SuMon/    # OpenAPI 规范接口文档 (.json 文件)
└── Protocol-SuMon/   # WebSocket、Agent 通信等协议文档 (.md 文件)
```

## 1. 开发计划 (Develop-plans/)

- 所有开发计划写成 `.md` 文件放在此目录下
- 每个计划文件命名格式: `YYYYMMDD-计划标题.md`
- 计划内容应包含：目标、任务清单、预期时间、依赖关系
- 模板见 `references/develop-plan-template.md`

## 2. 开发日志 (Develop-log/)

- 每次开发活动后记录日志，文件名格式: `YYYYMMDD-日志标题.md`
- 日志内容应包含：日期、操作人、改动内容摘要、涉及的文件
- 模板见 `references/develop-log-template.md`

## 3. OpenAPI 接口文档 (OpenApi-SuMon/)

- 使用 OpenAPI 3.0 规范，以 `.json` 文件格式存放
- 文件命名格式: `openapi-{模块名}.json` 或 `openapi.json`
- 每次新增或修改 API 接口时，同步更新对应的 OpenAPI 文档
- 模板见 `references/openapi-template.json`

## 4. 协议文档 (Protocol-SuMon/)

- WebSocket、Agent 通信、SSH 终端消息协议等写成 `.md` 文件放在此目录下
- WebSocket 协议文档命名为 `websocket-protocol.md`
- 文档中的服务器地址示例统一使用 `SERVER_IP_OR_DOMAIN`，不使用真实 IP 或内网 IP

## 工作流程

1. 接收开发任务后，先在 `Develop-plans/` 创建计划文档
2. 开发过程中，每完成一个功能模块，在 `Develop-log/` 记录开发日志
3. 新增或修改 API 时，同步更新 `OpenApi-SuMon/` 中的接口文档
4. 新增或修改 WebSocket/Agent 通信协议时，同步更新 `Protocol-SuMon/` 中的协议文档
5. 确保文档目录的内容保持一致

## Java 后端规范

本项目 Java 后端遵循《阿里巴巴 Java 开发手册》、Spring Boot 3.x 常规实践和项目需求文档中的约束。

### 命名规范

- 类名使用 UpperCamelCase，例如 `ServerController`、`JwtTokenService`。
- 方法名、参数名、局部变量名使用 lowerCamelCase。
- 常量使用 UPPER_SNAKE_CASE。
- 包名使用小写单词，主包为 `com.susumonitor.server`。
- 布尔变量不使用 `is` 前缀，例如使用 `enabled`，不使用 `isEnabled`。

### 目录规范

Java 后端工程目录固定为 `server-java-SuMon/`。

```text
server-java-SuMon/src/main/java/com/susumonitor/server/
├── config/
├── common/
├── security/
├── module/
├── websocket/
├── ssh/
└── scheduler/
```

业务模块放在 `module/` 下，每个模块推荐结构：

```text
模块名/
├── controller/
├── service/
├── mapper/
├── entity/
├── dto/
└── vo/
```

### Controller 规范

- Controller 只处理请求参数、认证上下文和响应封装。
- 业务规则放在 Service，不写在 Controller 中。
- 请求参数使用 DTO，响应对象使用 VO。
- 所有 REST API 使用统一响应结构。

### Service 规范

- Service 负责业务规则、事务、跨表协作和权限边界。
- 不用异常做正常业务流程控制。
- 捕获异常后必须处理或继续向上抛出统一业务异常。
- 涉及数据库写操作时明确事务边界。

### 数据库规范

- 数据库变更使用 Flyway 管理。
- SQL 查询禁止使用 `SELECT *`，必须指定具体字段。
- 密码只保存 BCrypt 哈希。
- SSH 密码、私钥、私钥口令必须使用 AES-256-GCM 加密存储。
- 日志不得输出密码、Token、SSH 私钥、SSH 密码、AES 密钥等敏感信息。

### 安全规范

- JWT 密钥、AES-GCM 密钥、Agent 注册密钥只从配置或环境变量读取，不硬编码。
- 管理员接口必须校验 `admin` 角色。
- 普通 `approved user` 只允许查看服务器，不允许新增、修改、删除或执行 SSH 测试。
- 服务器删除采用软删除，不物理删除记录。

### 测试规范

- 单元测试使用 JUnit 5。
- Service 隔离测试可使用 Mockito。
- 新增核心业务规则时应补充对应测试。
- 提交前至少执行 Maven 测试命令。

### 文档同步

- 新增或修改 REST API 时同步更新 OpenAPI 文档。
- 新增或修改 WebSocket、Agent、SSH 终端协议时同步更新协议文档。
- 每次开发活动完成后在 `docs-SuMon/Develop-log/` 记录开发日志。
