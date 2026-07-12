---
name: mysql-standards
description: MySQL 规范。当在 SuSuMonitor 项目目录下进行数据库设计或编写 SQL 时使用。触发场景包括：(1) 数据库表设计，(2) 编写 SQL 语句，(3) 索引设计，(4) Flyway 迁移脚本。
---

# MySQL 规范

## 1. 建表规范

### 命名
- 表名：小写，下划线分隔，复数形式，如 `users`、`alert_rules`
- 字段名：小写，下划线分隔，如 `created_at`、`server_id`
- 索引名：`idx_表名_字段名`，如 `idx_metrics_server_id`
- 唯一索引：`uk_表名_字段名`，如 `uk_users_username`

### 字段类型
- 主键：`BIGINT UNSIGNED AUTO_INCREMENT`
- 字符串：`VARCHAR(n)`，不超长用 `VARCHAR`，不用 `CHAR`
- 时间：`DATETIME`，不用 `TIMESTAMP`（2038 年溢出风险）
- 布尔：`TINYINT(1)`
- 金额：`DECIMAL(10,2)`，不用 `FLOAT`
- 状态枚举：`VARCHAR(20)` 或 `TINYINT`，不用 `ENUM`

### 建表示例

```sql
CREATE TABLE `servers` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '服务器名称',
    `host` VARCHAR(255) NOT NULL COMMENT '服务器地址',
    `status` VARCHAR(20) NOT NULL DEFAULT 'offline' COMMENT '状态: online/offline',
    `ssh_host` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'SSH 地址',
    `ssh_port` INT UNSIGNED NOT NULL DEFAULT 22 COMMENT 'SSH 端口',
    `ssh_user` VARCHAR(100) NOT NULL DEFAULT '' COMMENT 'SSH 用户名',
    `ssh_auth_type` VARCHAR(20) NOT NULL DEFAULT 'private_key' COMMENT 'SSH 认证方式: password/private_key',
    `ssh_password_encrypted` TEXT COMMENT 'SSH 密码（AES-256-GCM 加密）',
    `ssh_private_key_encrypted` TEXT COMMENT 'SSH 私钥（AES-256-GCM 加密）',
    `ssh_private_key_passphrase_encrypted` TEXT COMMENT 'SSH 私钥口令（AES-256-GCM 加密）',
    `last_heartbeat` DATETIME COMMENT '最后心跳时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_servers_host` (`host`),
    KEY `idx_servers_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器表';
```

## 2. 索引规范

- 每个表必须有主键
- WHERE / JOIN / ORDER BY 字段建索引
- 联合索引字段顺序：区分度高的在前
- 避免在索引列上使用函数
- 定期分析慢查询，优化索引

## 3. SQL 编写规范

- 关键字大写：`SELECT`、`FROM`、`WHERE`
- 禁止使用 `SELECT *`，必须指定具体字段
- 使用参数化查询，防止 SQL 注入
- 批量操作使用 `IN` 或 `VALUES` 多行，不用循环
- 分页查询使用 `LIMIT offset, count`，大数据量用游标分页

## 4. Flyway 迁移脚本

- 迁移脚本放在 `server-java-SuMon/src/main/resources/db/migration/`
- 文件名使用 Flyway 规范，例如 `V1__create_users_table.sql`
- 每个迁移脚本只做一组相关表结构变更
- 已执行到共享环境的迁移脚本不得直接修改，应通过新的版本脚本变更

## 5. 其他

- 所有表使用 InnoDB 引擎
- 字符集使用 `utf8mb4`
- 每个字段必须有 COMMENT
- 避免使用外键约束，应用层维护关联关系
- 大表考虑分表或分区（如 metrics 表）
