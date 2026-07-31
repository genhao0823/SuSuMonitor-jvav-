# SuSuMonitor Java 云端部署

本手册覆盖计划 T5 的 Java 运行资产，目标为 OpenCloudOS 9.6、宝塔 Nginx 和本机 MySQL。它不执行真实部署，也不替代生产变更确认。

## 运行边界

- Java 以 `susumonitor` 专用非 root 用户运行，只监听 `127.0.0.1:18080`。
- Nginx 是唯一公网入口；只对公网开放 HTTP/HTTPS 所需端口，`18080` 和 MySQL `3306` 不得开放公网。
- Java 终端输入、输出和 Agent Token 不得写入环境模板、日志、Nginx 配置或运维记录。
- 生产秘密只保存在 `/etc/susumonitor/server.env`，权限必须为 `root:root` 和 `0600`。
- 当前终端中继只支持单 Java 实例。不得在负载均衡器后横向启动多个实例。

## 上线前确认

在目标主机执行前，先由负责人明确确认以下事项：

1. 目标主机、域名、宝塔站点配置文件和 Java JAR 发布目录。
2. Nginx 站点配置备份路径，且备份文件已存在、非空并可读。
3. MySQL 备份工具、生产库名称、备份路径、可用磁盘空间和恢复责任人。
4. 发布窗口、回滚窗口和允许的服务中断时间。
5. 防火墙规则，确认 Java `18080` 与 MySQL `3306` 不会暴露到公网。

生产数据库备份、Nginx 配置修改、Flyway 迁移和服务重启都属于真实环境操作，必须在上述范围确认后单独执行。

## 构建发布包

在受信任的构建环境、`server-java-SuMon` 目录执行：

```bash
./mvnw clean package
sha256sum target/server-java-SuMon-0.0.1-SNAPSHOT.jar
```

将经校验的 JAR 上传到目标主机的版本目录，例如 `/opt/susumonitor/releases/RELEASE_ID/`。保留当前运行版本，发布目录不可由 `susumonitor` 用户写入。

## 首次安装

以下命令是人工操作步骤。执行前必须替换路径并确认目标主机；不要将真实密钥放到命令行历史或终端录屏中。

```bash
sudo useradd --system --home-dir /var/lib/susumonitor --shell /sbin/nologin susumonitor
sudo install -d -o root -g root -m 0755 /opt/susumonitor/server
sudo install -d -o root -g root -m 0700 /etc/susumonitor
sudo install -d -o susumonitor -g susumonitor -m 0700 /var/lib/susumonitor
sudo install -m 0600 -o root -g root deploy/susumonitor-server.env.example /etc/susumonitor/server.env
sudo install -m 0644 deploy/susumonitor-server.service /etc/systemd/system/susumonitor-server.service
```

在受控编辑器中填写 `/etc/susumonitor/server.env` 的实际值。必须保持 `APP_ENV=prod`、`SERVER_ADDRESS=127.0.0.1`，并将 `CORS_ALLOWED_ORIGINS` 设为实际 HTTPS 域名。

创建当前版本 JAR 的只读链接后执行：

```bash
sudo ln -sfn /opt/susumonitor/releases/RELEASE_ID/server-java-SuMon-0.0.1-SNAPSHOT.jar /opt/susumonitor/server/susumonitor-server.jar
sudo systemctl daemon-reload
sudo systemctl enable --now susumonitor-server
sudo systemctl status susumonitor-server --no-pager
```

应用日志由 journald 管理。使用 `sudo journalctl -u susumonitor-server` 检查启动错误；不得提高生产日志等级以记录 WebSocket 内容。

## Nginx 合并与验证

1. 备份宝塔当前站点配置并验证备份可读。
2. 将 `nginx-susumonitor.conf.example` 的 location 块合并到对应 HTTPS `server` 块；不得覆盖整份宝塔配置。
3. 保留宝塔管理的 TLS 配置，并替换 Vue 静态目录占位符。
4. 在目标主机执行 `nginx -t`，通过后再由宝塔或 `systemctl reload nginx` 重载。
5. 从公网验证 HTTPS、`/api/health`、`/api/ready`、`/ws/agent` 和 `/ws/monitor`；分别记录结果。

Nginx 默认访问日志不包含 WebSocket 帧正文。不要为排障记录终端消息体、Ticket、JWT 或 Agent Token。

## 数据库备份与发布

在执行会触发 Flyway 的首次启动或版本升级前：

1. 确认目标数据库仅为本次批准的生产库，且记录当前 Flyway 版本。
2. 使用目标环境已批准的备份工具创建逻辑备份，不在命令行或日志中暴露密码。
3. 验证备份文件存在、大小大于零且可读取；在开发日志中记录备份路径和校验结果，不记录凭据。
4. 再启动应用，由 Flyway 执行向前迁移；禁止修改或重置已执行的迁移历史。
5. 检查 `GET /api/health`、`GET /api/ready`、`systemctl status` 和 Flyway 日志后才宣布发布完成。

> **2026-07-31 更新（MVP-8）**：本仓库现已提供备份与恢复脚本——`deploy/backup.sh`（mysqldump 一致性全量 + server.env 密钥 + 校验轮换）与 `deploy/restore.sh`（备份包校验 + 覆盖式恢复 + 验证清单），操作见 `docs-SuMon/Use-manual/SuSuMonitor-备份与恢复手册.md`。脚本同样要求先确认目标与影响范围，恢复前必须停止应用。

## 升级与回滚

升级前保留当前 JAR 路径和已验证的数据库备份。升级仅替换 JAR 软链接后重启服务：

```bash
sudo ln -sfn /opt/susumonitor/releases/NEW_RELEASE/server-java-SuMon-0.0.1-SNAPSHOT.jar /opt/susumonitor/server/susumonitor-server.jar
sudo systemctl restart susumonitor-server
```

若新版本未通过健康检查，先停止进一步流量变更，再将软链接恢复到先前 JAR 并重启。已执行的 Flyway 向前迁移不能通过替换 JAR 自动回滚；数据库恢复必须在明确影响范围、确认备份可用并取得最终批准后单独执行。

## 验收记录边界

仓库内可验证的内容是 Maven Wrapper、JAR 构建和部署文件静态结构。目标主机 systemd、Nginx、MySQL 备份、HTTPS/WSS、公网防火墙和真实 Agent 链路必须在真实环境中单独验证和记录。
