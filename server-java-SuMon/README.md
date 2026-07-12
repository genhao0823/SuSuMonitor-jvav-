# SuSuMonitor Java Backend

SuSuMonitor Java 后端工程，基于 Java 21、Spring Boot 3.4.x 和 Maven。

## 一、技术栈

- Java 21 LTS
- Spring Boot 3.4.x
- Spring Web MVC
- Spring Security
- Spring WebSocket
- Hibernate Validator
- MyBatis-Plus
- MySQL 8.4
- Flyway
- springdoc-openapi
- JUnit 5

Java 代码必须遵循《阿里巴巴 Java 开发手册》。

## 二、本机配置

本机 MySQL 初始化脚本：

```text
../scripts/local-mysql-init.sql
```

当前本机开发数据库约定：

| 项 | 值 |
|----|----|
| 数据库地址 | `127.0.0.1:3306` |
| 数据库名 | `susumonitor` |
| 数据库用户 | `susumonitor` |
| 本机开发密码 | `732682` |

该密码仅用于本机开发，生产环境必须替换。

## 三、环境变量

参考 `.env.example`。真实 `.env` 不提交 Git。

关键密钥必须通过本机环境或部署平台注入：

- `JWT_SECRET`
- `AES_GCM_KEY`
- `AGENT_REGISTER_KEY`

## 四、构建和测试

```bash
mvn test
```

## 五、启动

基础系统接口已实现，可使用：

```bash
mvn spring-boot:run
```

目标验证：

```bash
curl http://localhost:18080/api/health
curl http://localhost:18080/api/ready
```

`/api/health` 不依赖数据库，`/api/ready` 会检查数据库连接。

## 六、Apifox 接口文档

OpenAPI 3.0 文档位置：

```text
../docs-SuMon/OpenApi-SuMon/openapi-system.json
```

在 Apifox 中导入该文件后，使用本机环境：

```text
baseUrl = http://localhost:18080
```

当前文档包含：

- `GET /api/health`
- `GET /api/ready`
- 统一成功响应
- 统一错误响应
- `X-Request-ID` 响应头

## 七、当前阶段

当前已完成 Java 后端工程骨架、统一响应、基础错误码、全局异常处理、request_id、`/api/health` 和 `/api/ready`。认证、管理员审核、服务器 CRUD、SSH 凭据加密和 SSH 测试将在后续阶段实现。
