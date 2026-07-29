#!/usr/bin/env bash
# SuSuMonitor Java Backend IPv4 本机 smoke 验证脚本
#
# 仅验证本机 127.0.0.1:18080 的最小可观测性：
# - 监听地址
# - /api/health
# - /api/ready
# - 不依赖 MySQL、AES/JWT 密钥或 Flyway 之外的运行时数据。
# - 不发起任何登录或写入操作。
#
# 使用方法（云服务器或本机）：
#   chmod +x deploy/smoke-127.0.0.1.sh
#   deploy/smoke-127.0.0.1.sh
#
# 可选环境变量：
#   SUSUMONITOR_SMOKE_BASE_URL   默认 http://127.0.0.1:18080
#   SUSUMONITOR_SMOKE_TIMEOUT    curl 超时秒数，默认 10

set -euo pipefail

BASE_URL="${SUSUMONITOR_SMOKE_BASE_URL:-http://127.0.0.1:18080}"
TIMEOUT="${SUSUMONITOR_SMOKE_TIMEOUT:-10}"

log() { printf '[smoke] %s\n' "$*"; }
fail() { printf '[smoke] FAIL: %s\n' "$*" >&2; exit 1; }

log "verifying IPv4 loopback listener at ${BASE_URL}"

if command -v ss >/dev/null 2>&1; then
    if ! ss -ltn "( sport = :18080 )" | grep -Eq '127\.0\.0\.1:18080|\[::ffff:127\.0\.0\.1\]:18080|0\.0\.0\.0:18080|\*:18080|\[::\]:18080'; then
        fail "127.0.0.1:18080 is not listening; start the Java backend first."
    fi
elif command -v netstat >/dev/null 2>&1; then
    if ! netstat -ltn 2>/dev/null | grep -Eq '127\.0\.0\.1:18080|0\.0\.0\.0:18080'; then
        fail "127.0.0.1:18080 is not listening; start the Java backend first."
    fi
else
    log "WARN: neither ss nor netstat available, skipping listener check"
fi

log "GET /api/health"
HEALTH="$(curl --silent --show-error --fail --max-time "$TIMEOUT" "${BASE_URL}/api/health")"
STATUS="$(printf '%s' "$HEALTH" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p')"
if [ "$STATUS" != "UP" ]; then
    fail "/api/health did not return status=UP: $HEALTH"
fi

log "GET /api/ready"
READY="$(curl --silent --show-error --fail --max-time "$TIMEOUT" "${BASE_URL}/api/ready" || true)"
if [ -z "$READY" ]; then
    log "WARN: /api/ready failed; database may not be reachable"
else
    READY_STATUS="$(printf '%s' "$READY" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p')"
    if [ "$READY_STATUS" != "UP" ]; then
        log "WARN: /api/ready did not return status=UP: $READY"
    fi
fi

log "all smoke checks passed"