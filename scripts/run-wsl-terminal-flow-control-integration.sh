#!/usr/bin/env bash
# Runs one isolated terminal flow-control scenario in a short-lived credential session.
set -euo pipefail

readonly AGENT_BINARY="/tmp/susumonitor-terminal-flow-control/susumonitor-agent-wsl"
readonly INTEGRATION_BINARY="/tmp/susumonitor-terminal-flow-control/terminal-flow-control-integration"
readonly REPOSITORY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly AGENT_PROJECT_ROOT="${REPOSITORY_ROOT}/agent-go-SuMon"
readonly GO_BINARY="/opt/go/bin/go"

if [[ ! -x "$GO_BINARY" ]]; then
    echo "Required WSL Go binary is unavailable: $GO_BINARY"
    exit 1
fi

# WSL may discard /tmp after the previous session exits, so build the private binaries per run.
mkdir -p "$(dirname "$AGENT_BINARY")"
chmod 700 "$(dirname "$AGENT_BINARY")"
"$GO_BINARY" -C "$AGENT_PROJECT_ROOT" build -o "$AGENT_BINARY" ./cmd/susumonitor-agent
"$GO_BINARY" -C "$AGENT_PROJECT_ROOT" build -o "$INTEGRATION_BINARY" ./cmd/terminal-flow-control-integration
chmod 700 "$AGENT_BINARY" "$INTEGRATION_BINARY"

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
