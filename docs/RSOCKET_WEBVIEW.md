# RSocket webview protocol

The Electron/Vite UI talks to Karaf through a single WebSocket carrying RSocket at path `/rsocket`. One connection is **full duplex**: multiple `requestStream` subscriptions, `fireAndForget` frames, `requestResponse` calls, and a `requestChannel` may run concurrently; do not serialize everything on one client-side lock.

## Envelope

- **Data**: JSON with `method`, optional `params`, optional `id` (same shape as before).
- **Metadata** (optional dual-read): UTF-8 string equal to `method`. If JSON omits `method`, the server uses metadata as the route. Clients should send **both** for compatibility.

## Interaction models

| Model | When to use |
|-------|-------------|
| `requestResponse` | Must return success/error to the UI in one round trip. |
| `requestStream` | Long-lived server → client events (`game.events`, `machine.status.stream`, …). |
| `fireAndForget` | Best-effort side effects (telemetry, hints) where no application-level ack is required. |
| `requestChannel` | Bidirectional streams (see `diagnostics.stream`). |

## Stream contracts (loss and demand)

- **`machine.status.stream`**: Per-machine flux uses `onBackpressureLatest()` before mapping to payloads; slow consumers may miss intermediate status updates. Wildcard mode merges status heartbeats with latest-only pressure on the status side.
- **`EventBridgeImpl` relay**: Uses `onBackpressureBuffer(8192)`; overflow increments `relayDroppedCount` (see `events.status` on the bridge).
- **Client `subscribe()`**: Uses `request(n)` / `initialRequestN` (default 64) to bound in-flight items; tune for very chatty streams.

## Diagnostics channel

- **Method / channel name**: `diagnostics.stream` (initial outbound frame `method` must match).
- **Initial `params`**: `pattern` (default `sokybot/**`), optional `machineId`.
- **Inbound commands** (`params`): `action` = `setPattern` \| `setMachine` \| `pause` \| `resume` \| `ping` (plus `pattern` / `machineId` where relevant).
- **Outbound**: `type: bridge.event` for filtered `IEventBridge` events; `type: diagnostics.command` for command acknowledgements.

## Reference code

- Server entry: [`RSocketServerService.java`](../ui/sokybot-webview/src/main/java/org/sokybot/webview/RSocketServerService.java)
- Registry: [`RSocketHandlerRegistry.java`](../ui/sokybot-webview/src/main/java/org/sokybot/webview/RSocketHandlerRegistry.java)
- Client: [`RSocketClient.ts`](../ui/sokybot-webview/src/main/frontend/src/RSocketClient.ts)
