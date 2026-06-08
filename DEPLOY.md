# Running & Deploying SIA DC-09 Servers

This guide covers building, running locally, and deploying the test servers
to the remote Linux host `wanchai-j1900`.

---

## Deploy scripts

| Script | Server class | Remote directory | Ports |
|---|---|---|---|
| `deploy-udp-server.sh` | `UdpServerTestManual` | `/home/wanchai/sia-dc-09-udp-server` | UDP 3061 |
| `deploy-both-server.sh` | `BothTcpUdpServerTestManual` | `/home/wanchai/sia-dc-09-both-server` | TCP+UDP 3061, TCP+UDP 3062 |

Both scripts share common logic in `_deploy-lib.sh`.

---

## Prerequisites

| Requirement | Version | Path |
|---|---|---|
| JDK 25 (macOS) | 25.0.2 | `/Users/wanchai/.jdk/jdk-25.0.2/jdk-25.0.2+10/Contents/Home` |
| Maven | 3.9.16 | `/Users/wanchai/.maven/maven-3.9.16/bin/mvn` |
| JDK 25 (Linux) | 25.0.2 | Uploaded automatically on first deploy to `/home/wanchai/.jdk/jdk-25.0.2+10` |

SSH access to `wanchai-j1900` is passwordless via `~/.ssh/id_ed25519`
(configured in `~/.ssh/config`).

---

## Build

```bash
JAVA_HOME=/Users/wanchai/.jdk/jdk-25.0.2/jdk-25.0.2+10/Contents/Home \
  /Users/wanchai/.maven/maven-3.9.16/bin/mvn install -DskipTests -q
```

---

## Run locally (VS Code)

Open the **Run and Debug** panel and select a launch configuration:

| Configuration | Description |
|---|---|
| `UdpServerTestManual` | UDP server on port 3061 |
| `TcpServerTestManual` | TCP server on port 3061 |
| `BothTcpUdpServerTestManual` | TCP+UDP on ports 3061 and 3062 |
| `ManualUdpClientTest` | Test UDP client |

All configurations use JDK 25 at
`/Users/wanchai/.jdk/jdk-25.0.2/jdk-25.0.2+10/Contents/Home/bin/java`.

---

## Deploy to `wanchai-j1900`

### One-time setup — Linux JDK bundle

The remote has no internet access. Pre-download the Linux JDK 25 bundle once:

```bash
curl -fL "https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.2%2B10/OpenJDK25U-jdk_x64_linux_hotspot_25.0.2_10.tar.gz" \
  -o /tmp/jdk-25-linux-x64.tar.gz --progress-bar
```

Cached at `/tmp/jdk-25-linux-x64.tar.gz`. The scripts detect if JDK 25 is
already installed on the remote and skip the upload automatically.

---

### `UdpServerTestManual` (UDP only)

**Deploy:**
```bash
./deploy-udp-server.sh
```

**Deploy and start interactively:**
```bash
./deploy-udp-server.sh --run
```

**Run in background:**
```bash
ssh wanchai-j1900 'nohup /home/wanchai/sia-dc-09-udp-server/run.sh > ~/sia-dc-09-udp.log 2>&1 &'
```

**View logs:**
```bash
ssh wanchai-j1900 'tail -f ~/sia-dc-09-udp.log'
```

**Verify:**
```bash
ssh wanchai-j1900 'ss -ulnp | grep 3061'
```

**Stop:**
```bash
ssh wanchai-j1900 'pkill -f UdpServerTestManual'
```

---

### `BothTcpUdpServerTestManual` (TCP + UDP)

**Deploy:**
```bash
./deploy-both-server.sh
```

**Deploy and start interactively:**
```bash
./deploy-both-server.sh --run
```

**Run in background:**
```bash
ssh wanchai-j1900 'nohup /home/wanchai/sia-dc-09-both-server/run.sh > ~/sia-dc-09-both.log 2>&1 &'
```

**View logs:**
```bash
ssh wanchai-j1900 'tail -f ~/sia-dc-09-both.log'
```

**Verify:**
```bash
ssh wanchai-j1900 'ss -tlnp; ss -ulnp' | grep -E '3061|3062'
```

Expected output (4 lines — TCP and UDP for each port):
```
LISTEN  *:3061   ...  ("java",pid=<PID>...)
LISTEN  *:3062   ...  ("java",pid=<PID>...)
UNCONN  *:3061   ...  ("java",pid=<PID>...)
UNCONN  *:3062   ...  ("java",pid=<PID>...)
```

**Stop:**
```bash
ssh wanchai-j1900 'pkill -f BothTcpUdpServerTestManual'
```

---

## What the deploy scripts do

1. **Build** — `mvn install -DskipTests` with JDK 25
2. **Collect deps** — copies all runtime + test-scope JARs to `target/deploy-lib/`
3. **Stage** — assembles `target/deploy-staging/` with `lib/`, `classes/`, and a self-contained `run.sh`
4. **JDK check** — uploads and installs Temurin 25 from local bundle if not already on remote
5. **Deploy** — `rsync` to the remote directory
6. *(with `--run`)* — starts the server interactively over SSH

### Remote layout (both servers follow same structure)

```
/home/wanchai/sia-dc-09-{udp,both}-server/
├── classes/    # compiled test classes
├── lib/        # all dependency JARs + sia-dc-09-server JAR
└── run.sh      # self-contained start script
```

---

## Notes

- **Netty `sun.misc.Unsafe` warning** — expected on Java 24+. Netty 4.2
  falls back automatically; the server works correctly.
- **Firewall** — ensure `wanchai-j1900` allows inbound TCP/UDP on the
  required ports if connecting from external devices.
