#!/usr/bin/env bash
# SuSuMonitor Java 后端一次性安装脚本（仅在不重复执行的前提下使用）
#
# 此脚本以 root 身份执行：
#   1. 创建专用 susumonitor 系统用户
#   2. 创建 /opt/susumonitor/server 和 /var/lib/susumonitor
#   3. 上传 server.env（占位符须由运维手工填写）
#   4. 安装 systemd unit
#
# 凭据、JWT/AES/Agent 密钥不得写入此文件或仓库。
# 仅当目标主机唯一用于本次部署且已备份现有 /opt/susumonitor 时执行。

set -euo pipefail

RELEASE_ID="${RELEASE_ID:-REPLACE_WITH_RELEASE_ID}"
RELEASES_DIR="${RELEASES_DIR:-/opt/susumonitor/releases}"
SERVER_DIR="${SERVER_DIR:-/opt/susumonitor/server}"
ENV_FILE="${ENV_FILE:-/etc/susumonitor/server.env}"
JAR_PATH="${JAR_PATH:-target/server-java-SuMon-0.0.1-SNAPSHOT.jar}"

if [ ! -f "$JAR_PATH" ]; then
    echo "[install] FATAL: JAR not found at $JAR_PATH" >&2
    exit 1
fi

if ! id -u susumonitor >/dev/null 2>&1; then
    useradd --system --home-dir /var/lib/susumonitor --shell /sbin/nologin susumonitor
fi

install -d -o root -g root -m 0755 "$RELEASES_DIR/$RELEASE_ID"
install -d -o root -g root -m 0755 "$SERVER_DIR"
install -d -o root -g root -m 0700 /etc/susumonitor
install -d -o susumonitor -g susumonitor -m 0700 /var/lib/susumonitor

install -m 0644 -o root -g root deploy/susumonitor-server.service /etc/systemd/system/susumonitor-server.service
if [ ! -f "$ENV_FILE" ]; then
    install -m 0600 -o root -g root deploy/susumonitor-server.env.example "$ENV_FILE"
    echo "[install] installed $ENV_FILE from template; must edit secrets manually"
fi

install -m 0644 -o root -g root "$JAR_PATH" "$RELEASES_DIR/$RELEASE_ID/server-java-SuMon-0.0.1-SNAPSHOT.jar"

ln -sfn "$RELEASES_DIR/$RELEASE_ID/server-java-SuMon-0.0.1-SNAPSHOT.jar" "$SERVER_DIR/susumonitor-server.jar"

systemctl daemon-reload
systemctl enable --now susumonitor-server
systemctl status susumonitor-server --no-pager