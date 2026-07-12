---
name: git-conventions
description: Git 提交规范。当在 SuSuMonitor 项目目录下进行 Git 操作时使用。触发场景包括：(1) 提交代码，(2) 创建分支，(3) 编写 commit message，(4) 合并代码。
---

# Git 提交规范

## 1. Commit Message 格式

遵循 Conventional Commits 规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

### type 类型

| type | 说明 |
|------|------|
| feat | 新功能 |
| fix | 修复 bug |
| docs | 文档变更 |
| style | 代码格式（不影响逻辑） |
| refactor | 重构 |
| perf | 性能优化 |
| test | 增加/修改测试 |
| chore | 构建/工具/依赖变更 |
| ci | CI/CD 变更 |

### scope 范围

| scope | 说明 |
|------|------|
| server | 后端服务 |
| agent | Agent 采集程序 |
| web | Web 前端 |
| android | Android 客户端 |
| docs | 文档 |
| deploy | 部署相关 |

### 示例

```
feat(server): 添加用户登录接口

实现 JWT 认证，包含登录和注册接口。
Token 有效期 24 小时，支持 Redis 黑名单。

Closes #1
```

```
fix(web): 修复仪表盘 CPU 图表不实时更新的问题
```

```
docs(readme): 更新项目 README 和部署文档
```

## 2. 分支命名

| 分支类型 | 命名格式 | 示例 |
|------|---------|------|
| 主分支 | main | main |
| 功能分支 | feat/功能描述 | feat/server-auth |
| 修复分支 | fix/修复描述 | fix/web-chart-update |
| 文档分支 | docs/文档描述 | docs/api-doc |
| 部署分支 | deploy/部署描述 | deploy/k8s-setup |

## 3. 提交粒度

- 一个 commit 只做一件事
- 硬性要求：每完成一个模块功能，必须提交一次本地 Git commit
- 模块功能包括但不限于：后端接口模块、数据库迁移模块、认证模块、服务器管理模块、前端页面模块、Android 功能模块、文档阶段成果
- 不要提交未完成的功能代码到 main 分支
- 提交前确保代码能编译通过
- 提交前必须检查 `git status` 和 `git diff`，确认只提交本次模块相关文件
- 提交前必须确认没有 `.env`、密钥、证书、Token、SSH 私钥、数据库密码等敏感信息
- 本地提交后，如配置了本机备份 remote，按需要推送到 `backup`

## 4. 其他

- 不要提交 `node_modules`、`build`、`.env` 等文件
- 不要提交包含密码/密钥的配置文件
- 使用 `.gitignore` 排除不需要版本控制的文件
