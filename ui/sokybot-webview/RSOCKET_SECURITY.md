# RSocket / WebSocket security

The embedded RSocket server in `sokybot-webview` accepts WebSocket connections **without authentication**. Any client that can reach the RSocket port can invoke handlers such as `machine.start`, `machine.stop`, `machine.create`, `group.create`, and streams including `machine.status.stream`.

## Deployment guidance

- **Local / trusted networks only.** Do not expose the HTTP(S) port that serves `/rsocket` to the public internet without adding an authentication layer (e.g. pre-shared token in RSocket `setup` metadata, reverse proxy with auth, or VPN-only access).
- **Document** this assumption for operators: treat the control plane as equivalent to full access to all configured bots.

## Future hardening (optional)

- Validate a shared secret or session token on `ConnectionSetupPayload` before accepting requests.
- Bind the listener to `127.0.0.1` when remote access is not required.

## UI / backend alignment (Karaf, OSGi, RSocket)

- **`Bundle` Active ≠ handler registered.** Declarative Services may still be wiring `IRSocketHandler` services into `RSocketHandlerRegistry`. Post-deploy checks should call an **actual RSocket method** (e.g. `workspace.summary` or `system.info`), not only `bundle:list` / `feature:install` success.
- **Method not found: `workspace.summary`.** Usually **skew**: browser UI newer than the running `sokybot-webview` bundle. Redeploy the **same build** as the UI (atomic artifact / full feature refresh). The UI retries briefly, then may fall back to `group.list` + `machine.list` and show a **legacy workspace** banner.
- **SETUP payload.** Clients send JSON in the RSocket SETUP frame (`minProtocolApi`, `uiBuildId`). The server rejects the connection if `minProtocolApi` is greater than the supported API level (`WebviewProtocolConstants`).
- **`system.info`** returns `protocolApi` and `webviewBundleVersion` (OSGi bundle version) for support and skew diagnosis.
- **Karaf `data/cache` and SNAPSHOTs.** After `mvn install`, `bundle:update` with the same version can still load a **cached** JAR. Use **`bundle:watch`**, **`update --force`** (if available), or clear the relevant cache; see [`scripts/karaf-dev-bundle-refresh.sh`](../../scripts/karaf-dev-bundle-refresh.sh).
- **Feature upgrades.** Prefer **immutable** container/distro deploys. If updating features in place, refresh/clean so **orphaned bundles** do not leave duplicate or stale RSocket routes.
- **RSocket drops on bundle refresh.** Hot-updating `sokybot-webview` closes connections; the UI should **reconnect** (not the same as persistent `METHOD_NOT_FOUND` skew).
- **CI.** Full **OSGi** verification (e.g. Pax Exam + headless Karaf) is the strongest guardrail; plain JVM integration tests do not catch `Import-Package` or SCR wiring issues. This repo includes a **unit-level** `workspace.summary` registry smoke test as a minimum gate.
- **Post-deploy RSocket smoke (optional):** against a **running** backend, from repo root run [`scripts/rsocket-post-deploy-smoke.sh`](../../scripts/rsocket-post-deploy-smoke.sh) `ws://HOST:PORT/rsocket` (internally runs `RSocketWorkspaceSummarySmokeIT` with `-Dsokybot.rsocket.smoke.url=...`).
