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
- **UI Code**: Vite provides HMR for all frontend modules.
- **Groovy Scripts**: The engine natively hot-reloads `.groovy` files.

## Compile trigger (Node-like loop)
IDE **auto-build** usually writes **`target/classes`**, not **`target/*.jar`**. The JAR watcher stays idle until a bundle JAR is produced. Recommended:
- **IntelliJ**: File Watcher (or similar) on save to run `./mvnw package -DskipTests -pl <module> -am` for the module you edit; **[mvnd](https://github.com/apache/maven-mvnd)** (`mvnd package -DskipTests`) for faster incremental builds if installed.
- **VS Code**: A **task** bound to Java file save that runs the same Maven command for the relevant module.

## Maven cache in Docker
Compose mounts **`~/.m2` → `/var/maven/.m2`**. [`infra/scripts/soky`](../../infra/scripts/soky) uses **`get_maven_local_repo()`** so the writable check and **`-Dmaven.repo.local`** always match. **`SOKY_KARAF_REBUILD_ASSEMBLY_ON_START=1`** fails fast if that repo directory is not writable (e.g. read-only mount).

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

## Portainer, Karaf cache, and assembly (Docker backend start pipeline)

### What a container restart does
- **Restart** (e.g. Portainer) re-runs the entrypoint on the same bind-mounted repo. It does **not** pull a new image or run Maven unless you opt in below.

### Environment variables ([`docker-compose.yml`](../../docker-compose.yml))

| Variable | Role |
|----------|------|
| **`SOKY_KARAF_CLEAR_CACHE_ON_START=1`** (default **on**) | Removes only **`$KARAF_DATA/cache`** before Karaf. Safe for routine restarts; avoids ghost SNAPSHOT / OSGi skew. Does **not** delete **`system/org/sokybot`**. |
| **`SOKY_KARAF_REBUILD_ASSEMBLY_ON_START=1`** (default **off**) | Runs **`mvnw -B install -pl infra/sokybot-dist -am -P fast -DskipTests`** from **repo root** inside the container. **Exits 1** if Maven fails or if **`system/org/sokybot`** is still empty after success. **Use temporarily** to recover from a missing/empty `system/` tree; then unset to avoid a full rebuild on every `up`. |
| **`SOKY_KARAF_FRESH_OSGI_ON_START`** | **Deprecated.** If set to **`1`** without **`REBUILD_ASSEMBLY`**, **`backend start` exits with code 2** with a message to use **`CLEAR_CACHE`** and **`REBUILD_ASSEMBLY`** instead. |
| **`KARAF_USER`** | Optional. If the process is **root** and this is set (e.g. `1000:1000`), **`chown -R`** is applied to **`system/org/sokybot`** and **`KARAF_DATA`** after a successful rebuild. Typical compose runs as a single non-root user; then this is unused. |

### Fixed order (inside Docker)
1. Validate **`KARAF_DIST`** / **`KARAF_DATA`**; **`mkdir -p`** data dir if needed.  
2. Deprecation gate for **`FRESH_OSGI`** without **`REBUILD`**.  
3. **Rebuild** (if enabled): writable **`.m2`** check → Maven in **subshell** at **`ROOT_DIR`** → assert **`system/org/sokybot`** non-empty → optional **`chown`**.  
4. **Clear cache** (if enabled).  
5. If **rebuild did not run**, assert **`system/org/sokybot`** still non-empty (fail with hint to enable **`REBUILD`** or run **`soky build backend --fast`** on the host).  
6. Start Karaf.

### Operational notes
- **Interrupted Maven**: If a rebuild is stopped mid-flight, remove **`infra/sokybot-dist/target/assembly`** or run **`mvn clean`** from the host before retrying to avoid a half-written tree.  
- **Memory**: Full **`REBUILD`** can need **~4GB+** Docker RAM; OOM may show as exit **137**.  
- **Health checks**: If you probe port **8182**, use a long **`start_period`** when **`REBUILD_ASSEMBLY`** is enabled so the container is not marked unhealthy during Maven.  
- **Windows/macOS bind mounts**: Antivirus or IDE file locks on **`target/`** can make Maven fail; the script exits with a Maven error.  
- **Single-user images**: Current compose uses **`user: "${UID:-1000}:${GID:-1000}"`**; no user switch, so **`KARAF_USER`** is usually unset.

## Operational Rules
1. **JDK Version**: Always use JDK 21 (Temurin) for compatibility.
2. **Game Client**: The game client is mounted at `/app/game-client`. Use this path in the app.
3. **Restart Policy**: Containers are set to `unless-stopped`.
4. **External Network**: Connects to `proxy_net` for Nginx Proxy Manager integration.
