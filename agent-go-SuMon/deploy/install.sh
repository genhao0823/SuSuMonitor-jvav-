#!/bin/bash
#
# SuSuMonitor Agent 一键安装脚本。
#
# 用法：
#   sudo bash install.sh
#
# 前置条件：
#   1. 已在项目根目录执行 make build-linux 生成 bin/susumonitor-agent-linux-amd64。
#   2. 将二进制文件和本脚本一起传输到目标 Linux 服务器。
#   3. 以 root 权限执行。
#
# 本脚本完成以下操作：
#   - 创建 susumonitor 系统用户
#   - 拷贝二进制到 /usr/local/bin/
#   - 创建配置目录 /etc/susumonitor/
#   - 创建日志目录 /var/log/susumonitor/
#   - 拷贝 systemd service 文件
#   - 拷贝 logrotate 配置
#   - 启用开机自启
#
# 本脚本不会自动启动 Agent，管理员需要先修改配置后手动启动。

set -euo pipefail

# 脚本所在目录，用于定位同目录下的部署文件。
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 颜色输出。
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }

# 检查 root 权限。
if [ "$(id -u)" -ne 0 ]; then
    error "This script must be run as root. Use: sudo bash install.sh"
    exit 1
fi

# 检查二进制文件是否存在。
BINARY="${SCRIPT_DIR}/susumonitor-agent-linux-amd64"
if [ ! -f "${BINARY}" ]; then
    # 兼容从项目 deploy 目录直接安装的场景。
    BINARY="${SCRIPT_DIR}/../bin/susumonitor-agent-linux-amd64"
fi
if [ ! -f "${BINARY}" ]; then
    error "Binary not found. Run 'make build-linux' first, then copy the binary and this script to the server."
    exit 1
fi

INSTALL_DIR="/usr/local/bin"
CONFIG_DIR="/etc/susumonitor"
LOG_DIR="/var/log/susumonitor"
SERVICE_FILE="/etc/systemd/system/susumonitor-agent.service"
LOGROTATE_FILE="/etc/logrotate.d/susumonitor-agent"
BINARY_DEST="${INSTALL_DIR}/susumonitor-agent"

# 创建专用系统用户（如果不存在）。
if ! id -u susumonitor >/dev/null 2>&1; then
    info "Creating system user 'susumonitor'..."
    useradd --system --no-create-home --shell /usr/sbin/nologin susumonitor
else
    info "User 'susumonitor' already exists."
fi

# 拷贝二进制。
info "Installing binary to ${BINARY_DEST}..."
install -m 0755 "${BINARY}" "${BINARY_DEST}"

# 创建配置目录。
info "Creating config directory ${CONFIG_DIR}..."
mkdir -p "${CONFIG_DIR}"

# 拷贝环境变量模板（如果目标已存在则跳过，不覆盖已有配置）。
ENV_TEMPLATE="${SCRIPT_DIR}/agent.env"
if [ -f "${ENV_TEMPLATE}" ]; then
    if [ -f "${CONFIG_DIR}/agent.env" ]; then
        warn "${CONFIG_DIR}/agent.env already exists, skipping. Edit it manually if needed."
    else
        info "Installing env template to ${CONFIG_DIR}/agent.env..."
        install -m 0640 -o susumonitor -g susumonitor "${ENV_TEMPLATE}" "${CONFIG_DIR}/agent.env"
    fi
else
    warn "agent.env template not found in ${SCRIPT_DIR}. Create ${CONFIG_DIR}/agent.env manually."
fi

# 创建日志目录。
info "Creating log directory ${LOG_DIR}..."
mkdir -p "${LOG_DIR}"
chown susumonitor:susumonitor "${LOG_DIR}"
chmod 0755 "${LOG_DIR}"

# 拷贝 logrotate 配置。
LOGROTATE_SRC="${SCRIPT_DIR}/logrotate.conf"
if [ -f "${LOGROTATE_SRC}" ]; then
    info "Installing logrotate config to ${LOGROTATE_FILE}..."
    install -m 0644 "${LOGROTATE_SRC}" "${LOGROTATE_FILE}"
else
    warn "logrotate.conf not found in ${SCRIPT_DIR}. Skipping logrotate setup."
fi

# 拷贝 systemd service 文件。
SERVICE_SRC="${SCRIPT_DIR}/susumonitor-agent.service"
if [ -f "${SERVICE_SRC}" ]; then
    info "Installing systemd service to ${SERVICE_FILE}..."
    install -m 0644 "${SERVICE_SRC}" "${SERVICE_FILE}"
else
    error "susumonitor-agent.service not found in ${SCRIPT_DIR}."
    exit 1
fi

# 重新加载 systemd 配置。
info "Reloading systemd daemon..."
systemctl daemon-reload

# 启用开机自启。
info "Enabling susumonitor-agent service..."
systemctl enable susumonitor-agent

echo ""
echo "========================================"
echo "  SuSuMonitor Agent installed successfully"
echo "========================================"
echo ""
echo "Next steps:"
echo "  1. Edit config:  sudo nano ${CONFIG_DIR}/agent.env"
echo "  2. Start agent:   sudo systemctl start susumonitor-agent"
echo "  3. Check status:  sudo systemctl status susumonitor-agent"
echo "  4. View logs:     sudo journalctl -u susumonitor-agent -f"
echo "                   or: tail -f ${LOG_DIR}/agent.log"
echo ""
echo "Useful commands:"
echo "  Stop:    sudo systemctl stop susumonitor-agent"
echo "  Restart: sudo systemctl restart susumonitor-agent"
echo ""
