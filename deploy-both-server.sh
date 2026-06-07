#!/usr/bin/env bash
# Deploy BothTcpUdpServerTestManual to wanchai-j1900 (TCP+UDP ports 33200 & 33201).
# Usage: ./deploy-both-server.sh [--run]

set -euo pipefail
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
SERVER_MODULE="$PROJECT_ROOT/sia-dc-09-server"
# shellcheck source=_deploy-lib.sh
source "$PROJECT_ROOT/_deploy-lib.sh"

REMOTE_DIR="/home/wanchai/sia-dc-09-both-server"
MAIN_CLASS="ch.swissdotnet.siadc09.server.tests.BothTcpUdpServerTestManual"
RUN_AFTER=false
for arg in "$@"; do [[ "$arg" == "--run" ]] && RUN_AFTER=true; done

deploy_connect
deploy_build
deploy_collect_deps
deploy_ensure_jdk
deploy_stop   33200 33201
deploy_stage  "$MAIN_CLASS"
deploy_rsync  "$REMOTE_DIR"

echo ""
echo "=== Deploy complete ==="
echo "Run: ssh $REMOTE_USER@$REMOTE_HOST '$REMOTE_DIR/run.sh'"
$RUN_AFTER && deploy_run "$REMOTE_DIR" || true
