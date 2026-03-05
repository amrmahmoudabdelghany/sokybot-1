# Development Docker Environment

## Overview
The Sokybot development environment is containerized and optimized for JDK 21. It features automated hot-reloading for Java bundles and seamless CLI transparency between host and container.

## Service Mesh

| Service | Public URL | Internal Port | Purpose |
|---------|------------|---------------|---------|
| `sokybot-backend` | `api.sokybot.local` | 8182 | Java (Karaf) Backend (JDK 21) |
| `sokybot-webview-ui` | `sokybot.local` | 5173 | React Webview UI (Vite) |

## Automated Hot-Reloading
- **Java Bundles**: The backend container runs a watcher (`soky backend watch`) that detects JAR updates in `target/` directories and hotswaps them into Karaf using `bundle:update`.
- **UI Code**: Vite provides HMR for all frontend changes.
- **Groovy Scripts**: The engine natively hot-reloads `.groovy` files.

## CLI Transparency ("Docker-Aware")
The `./infra/scripts/soky` CLI on the host is aware of the running containers:
- Commands like `backend shell`, `backend logs`, and `backend watch` will automatically route through `docker exec` if a container is detected.
- You do NOT need to prefix commands with `docker exec`.

## Debugging & Diagnostics
- **Java Debugging**: Port `5005` is exposed for remote attachment.
- **Karaf Shell**: Port `8101` is exposed for SSH access.
- **Logs**: Use `./soky backend logs`.
- **Manual Hot-Reload**: Use `./soky backend watch` if the automatic one is stopped.

## Operational Rules
1. **JDK Version**: Always use JDK 21 (Temurin) for compatibility.
2. **Game Client**: The game client is mounted at `/app/game-client`. Use this path in the app.
3. **Restart Policy**: Containers are set to `unless-stopped`.
4. **External Network**: Connects to `proxy_net` for Nginx Proxy Manager integration.
