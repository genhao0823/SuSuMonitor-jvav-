---
name: susumonitor-docker
description: SuSuMonitor Docker 规范。当在 SuSuMonitor 项目目录下编写 Dockerfile 或 Docker Compose 配置时自动加载。触发场景：编写 Dockerfile、编写 docker-compose.yml、构建 Docker 镜像、镜像优化。
when_to_use: 编写 Dockerfile / docker-compose.yml / 构建镜像 / 镜像优化 时
user-invocable: true
---

# SuSuMonitor Docker 规范

## 1. Dockerfile 最佳实践

### 基础镜像

- 使用官方镜像，指定具体版本，避免 `latest`
- 优先使用 `alpine` 版本减小镜像体积
- Java 后端容器化时优先使用多阶段构建

### Java 后端构建提示

- 构建阶段使用 Maven 和 JDK 镜像。
- 运行阶段使用 JRE 镜像。
- 最终镜像只复制构建产物，不复制源码、测试报告、`.env` 或本地缓存。

### 前端构建示例

```dockerfile
# 构建阶段
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# 运行阶段
FROM nginx:1.27-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
RUN chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx /var/log/nginx /etc/nginx/conf.d \
    && touch /var/run/nginx.pid \
    && chown nginx:nginx /var/run/nginx.pid
USER nginx
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
```

## 2. 镜像优化

- 合并 RUN 指令减少层数
- 使用 `.dockerignore` 排除不需要的文件
- 不使用 root 用户运行容器，创建专用用户
- 清理包管理器缓存

## 3. docker-compose.yml 规范

- 使用 `version: '3.8'` 或更高
- 服务名使用项目名-服务名格式
- 敏感信息使用环境变量，不硬编码
- 使用 `depends_on` 和 `healthcheck` 确保启动顺序
- 数据卷挂载到宿主机，避免数据丢失

## 4. 安全

- **不要在镜像中硬编码密码、密钥、JWT_SECRET、AES-GCM 密钥、Agent 注册密钥**
- 不要以 root 权限运行容器
- 使用 `USER` 指令切换非 root 用户
- 定期更新基础镜像
- 通过 `docker run -e KEY=value` 或 Docker secrets 注入密钥

## 5. 镜像命名

- 格式：`镜像仓库/项目名/服务名:版本`
- 版本号使用 Git tag 或 commit hash
- 示例：`ghcr.io/susumonitor/server:v1.0.0`

## 6. 与项目集成

- 项目当前不直接走 Docker 部署（生产由 systemd + Nginx 部署，详见 `docs-SuMon/Handoff-SuMon/`）
- Dockerfile / docker-compose 主要用于本地开发辅助或未来容器化部署
- 真实部署资产（systemd unit / nginx.conf）放在 `server-java-SuMon/deploy/`
