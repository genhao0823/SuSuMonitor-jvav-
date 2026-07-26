#!/usr/bin/env bash
# Runs the isolated WSL Linux PTY verification in one short-lived credential session.
set -euo pipefail

readonly AGENT_BINARY="/tmp/susumonitor-pty-integration/susumonitor-agent-wsl"
readonly INTEGRATION_BINARY="/tmp/susumonitor-pty-integration/terminal-integration"

if [[ ! -x "$AGENT_BINARY" || ! -x "$INTEGRATION_BINARY" ]]; then
    echo "Required WSL integration binaries are unavailable."
    exit 1
fi

read -r -p "Isolated application administrator username: " username
read -r -s -p "Isolated application administrator password: " password
printf '\n'

export SUSUMONITOR_INTEGRATION_USERNAME="$username"
export SUSUMONITOR_INTEGRATION_PASSWORD="$password"
export SUSUMONITOR_INTEGRATION_AGENT_BINARY="$AGENT_BINARY"
unset username password

cleanup() {
    unset SUSUMONITOR_INTEGRATION_USERNAME
    unset SUSUMONITOR_INTEGRATION_PASSWORD
    unset SUSUMONITOR_INTEGRATION_AGENT_BINARY
}
trap cleanup EXIT

exec "$INTEGRATION_BINARY"
