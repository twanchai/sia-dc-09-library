#!/usr/bin/env bash
# Shared helpers for deploy-udp-server.sh and deploy-both-server.sh.
# Source this file; do not execute directly.

REMOTE_USER="wanchai"
REMOTE_HOST="wanchai-j1900"
MVN="/Users/wanchai/.maven/maven-3.9.16/bin/mvn"
JAVA_HOME_25="/Users/wanchai/.jdk/jdk-25.0.2/jdk-25.0.2+10/Contents/Home"
LOCAL_JDK_BUNDLE="/tmp/jdk-25-linux-x64.tar.gz"

# ---------------------------------------------------------------------------
# deploy_connect — open a multiplexed SSH master connection
deploy_connect() {
  SSH_SOCK="/tmp/ssh-mux-${REMOTE_HOST}.sock"
  SSH_OPTS="-o ControlMaster=auto -o ControlPath=$SSH_SOCK -o ControlPersist=120"
  echo "=== Connecting to $REMOTE_USER@$REMOTE_HOST ==="
  ssh $SSH_OPTS -fN "$REMOTE_USER@$REMOTE_HOST"
  trap 'ssh -O exit -o ControlPath="$SSH_SOCK" "$REMOTE_USER@$REMOTE_HOST" 2>/dev/null || true' EXIT
  echo "    Connected"
}

# ---------------------------------------------------------------------------
# deploy_build — build + install all modules
deploy_build() {
  echo ""
  echo "=== Step 1: Build ==="
  cd "$PROJECT_ROOT"
  JAVA_HOME="$JAVA_HOME_25" "$MVN" install -DskipTests -q
  echo "    Build OK"
}

# ---------------------------------------------------------------------------
# deploy_collect_deps — copy all test-scope deps into target/deploy-lib
deploy_collect_deps() {
  echo ""
  echo "=== Step 2: Collect dependencies ==="
  cd "$SERVER_MODULE"
  JAVA_HOME="$JAVA_HOME_25" "$MVN" dependency:copy-dependencies \
    -DincludeScope=test \
    -DoutputDirectory=target/deploy-lib \
    -q
  echo "    Done"
}

# ---------------------------------------------------------------------------
# deploy_stage MAIN_CLASS — assemble target/deploy-staging
deploy_stage() {
  local main_class="$1"
  echo ""
  echo "=== Step 3: Stage ==="
  STAGING="$SERVER_MODULE/target/deploy-staging"
  rm -rf "$STAGING"
  mkdir -p "$STAGING/lib" "$STAGING/classes"
  cp "$SERVER_MODULE/target/deploy-lib/"*.jar "$STAGING/lib/"
  cp "$SERVER_MODULE/target/sia-dc-09-server-"*.jar "$STAGING/lib/"
  cp -r "$SERVER_MODULE/target/test-classes/." "$STAGING/classes/"

  # resolve remote JAVA_HOME for run.sh
  local remote_java_home
  remote_java_home=$(ssh $SSH_OPTS "$REMOTE_USER@$REMOTE_HOST" bash << 'GET_HOME'
ls -d /home/wanchai/.jdk/jdk-25* 2>/dev/null | head -1
GET_HOME
)

  cat > "$STAGING/run.sh" << RUNSCRIPT
#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="\$(cd "\$(dirname "\$0")" && pwd)"
export JAVA_HOME="${remote_java_home}"
export PATH="\$JAVA_HOME/bin:\$PATH"
CP="\$SCRIPT_DIR/classes"
for jar in "\$SCRIPT_DIR/lib/"*.jar; do CP="\$CP:\$jar"; done
echo "Starting ${main_class}..."
exec java -cp "\$CP" ${main_class} "\$@"
RUNSCRIPT
  chmod +x "$STAGING/run.sh"
  echo "    Staged (main: $main_class)"
}

# ---------------------------------------------------------------------------
# deploy_ensure_jdk — install Temurin 25 on remote if missing
deploy_ensure_jdk() {
  echo ""
  echo "=== Step 4: Remote JDK check ==="
  local ver
  ver=$(ssh $SSH_OPTS "$REMOTE_USER@$REMOTE_HOST" bash << 'CHECK'
JDK_DIR=$(ls -d /home/wanchai/.jdk/jdk-25* 2>/dev/null | head -1 || true)
[[ -n "$JDK_DIR" && -x "$JDK_DIR/bin/java" ]] \
  && "$JDK_DIR/bin/java" -version 2>&1 | awk -F '"' 'NR==1{print $2}' \
  || echo ""
CHECK
)
  if [[ "$ver" == 25* ]]; then
    echo "    JDK 25 present ($ver)"
    return
  fi
  echo "    Installing JDK 25 from local bundle..."
  scp -o "ControlPath=$SSH_SOCK" "$LOCAL_JDK_BUNDLE" \
    "$REMOTE_USER@$REMOTE_HOST:/tmp/jdk-25-linux-x64.tar.gz"
  ssh $SSH_OPTS "$REMOTE_USER@$REMOTE_HOST" bash << 'INSTALL'
set -euo pipefail
mkdir -p /home/wanchai/.jdk && cd /home/wanchai/.jdk
tar -xzf /tmp/jdk-25-linux-x64.tar.gz
rm /tmp/jdk-25-linux-x64.tar.gz
JDK_DIR=$(ls -d /home/wanchai/.jdk/jdk-25* | head -1)
grep -qF "jdk-25" /home/wanchai/.bashrc 2>/dev/null || {
  echo "export JAVA_HOME=$JDK_DIR"        >> /home/wanchai/.bashrc
  echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /home/wanchai/.bashrc
}
"$JDK_DIR/bin/java" -version 2>&1 | head -1
INSTALL
  echo "    JDK 25 installed"
}

# ---------------------------------------------------------------------------
# deploy_rsync REMOTE_DIR — push staging to remote
deploy_rsync() {
  local remote_dir="$1"
  echo ""
  echo "=== Step 5: Deploy to $REMOTE_USER@$REMOTE_HOST:$remote_dir ==="
  ssh $SSH_OPTS "$REMOTE_USER@$REMOTE_HOST" "mkdir -p '$remote_dir'"
  rsync -az --delete -e "ssh -o ControlMaster=no -o ControlPath=$SSH_SOCK" \
    "$STAGING/" "$REMOTE_USER@$REMOTE_HOST:$remote_dir/"
  echo "    Deployed"
}

# ---------------------------------------------------------------------------
# deploy_stop PORTS... — kill any java process holding the given ports on remote
deploy_stop() {
  local ports=("$@")
  echo ""
  echo "=== Stopping existing servers on remote ==="
  local port_grep
  port_grep=$(printf '%s|' "${ports[@]}")
  port_grep="${port_grep%|}"
  ssh $SSH_OPTS "$REMOTE_USER@$REMOTE_HOST" bash << STOP
set -euo pipefail
PIDS=\$(ss -tlnp -ulnp 2>/dev/null \
  | grep -E ':($port_grep)' \
  | grep -oP '(?<=pid=)\d+' \
  | sort -u || true)
if [[ -n "\$PIDS" ]]; then
  echo "  Killing PIDs: \$PIDS"
  echo "\$PIDS" | xargs kill 2>/dev/null || true
  sleep 2
else
  echo "  No server processes found on ports $port_grep"
fi
STOP
}

# ---------------------------------------------------------------------------
# deploy_run REMOTE_DIR — start the server on the remote interactively
deploy_run() {
  local remote_dir="$1"
  echo ""
  echo "=== Starting server on $REMOTE_HOST (Ctrl-C to stop) ==="
  ssh $SSH_OPTS -t "$REMOTE_USER@$REMOTE_HOST" "$remote_dir/run.sh"
}
