# Development Docker Environment

## Overview
The Sokybot development environment is containerized and optimized for JDK 21. It features automated hot-reloading for Java bundles and seamless CLI transparency between host and container.

## Service Mesh

| Service | Public URL | Internal Port | Purpose |
|---------|------------|---------------|---------|
| `sokybot-backend` | `api.sokybot.local` | 8182 | Java (Karaf) Backend (JDK 21) |
| `sokybot-webview-ui` | `sokybot.local` | 5173 | React Webview UI (Vite) |

## Automated Hot-Reloading
- **Java Bundles (recommended)**: Run **`soky backend watch-host`** on the **host** while `sokybot-backend` is up. It watches `**/target/*.jar` with reliable filesystem events and runs `docker compose exec -T sokybot-backend` to Karaf with **`bundle:update <id> file:///app/...`** so SNAPSHOT cache skew is reduced. In-container `inotifywait` on bind mounts is optional (`SOKY_JAR_WATCH_IN_CONTAINER=1` + `backend start --watch`).
- **UI Code**: Vite provides HMR for all frontend changes.
- **Groovy Scripts**: The engine natively hot-reloads `.groovy` files.

## Compile trigger (Node-like loop)
IDE **auto-build** usually writes **`target/classes`**, not **`target/*.jar`**. The JAR watcher stays idle until a bundle JAR is produced. Recommended:
- **IntelliJ**: File Watcher (or similar) on save to run `./mvnw package -DskipTests -pl <module> -am` for the module you edit; **[mvnd](https://github.com/apache/maven-mvnd)** (`mvnd package -DskipTests`) for faster incremental builds if installed.
- **VS Code**: A **task** bound to Java file save that runs the same Maven command for the relevant module.

## Maven cache in Docker
Compose mounts **`~/.m2` → `/var/maven/.m2`**. The `soky` script passes **`-Dmaven.repo.local=/var/maven/.m2/repository`** when that directory exists so UID **1000** builds inside the container reuse the host cache (not `/root/.m2`).

## CLI Transparency ("Docker-Aware")
The `./infra/scripts/soky` CLI on the host is aware of the running containers:
- Commands like `backend shell`, `backend logs`, and `backend watch` will automatically route through `docker exec` if a container is detected.
- **`backend watch-host`** is an exception: it must run **on the host** (it drives `docker compose exec` itself).
- You do NOT need to prefix commands with `docker exec`.

## Debugging & Diagnostics
- **Java Debugging**: Port `5005` is exposed for remote attachment.
- **Karaf Shell**: Port `8101` is exposed for SSH access.
- **Logs**: Use `./soky backend logs`.
- **Manual Hot-Reload**: Prefer `./soky backend watch-host` from the host; use `./soky backend watch` only inside the container or with `SOKY_JAR_WATCH_IN_CONTAINER=1` if you accept bind-mount inotify limitations.

## Operational Rules
1. **JDK Version**: Always use JDK 21 (Temurin) for compatibility.
2. **Game Client**: The game client is mounted at `/app/game-client`. Use this path in the app.
3. **Restart Policy**: Containers are set to `unless-stopped`.
4. **External Network**: Connects to `proxy_net` for Nginx Proxy Manager integration.
