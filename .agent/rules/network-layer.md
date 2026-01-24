---
trigger: always_on
glob: "**/sokybot-proxy/**/*"
description: Network layer and proxy guidelines
---

# Network Layer Rules

## 1. Technology Stack
- **Library**: Netty 4.1.
- **Protocol**: Custom binary protocol (Silkroad Online) over TCP.
- **Encryption**: Blowfish (via `sokybot-security`).

## 2. Proxy Architecture (`sokybot-proxy`)
The proxy intercepts traffic between the Game Client and Game Server.
- `IProxyConnection`: Represents an active connection pair (Client ↔ Proxy ↔ Server).
- `IPacketPublisher`: Observable stream of packets.
- `IPacketObserver`: Consumer of packets.

## 3. Packet Handling
1. **Packet Structure**: defined in `org.sokybot.network.packet`.
   - `ImmutablePacket`: Read-only packet data.
   - `MutablePacket`: Writeable packet builder.
2. **Translation**: Use `IPacketTranslator` in `sokybot-game-events` to convert raw packets to high-level `IGameEvent`s.

## 4. Best Practices
- **Non-Blocking**: Netty handlers must be non-blocking. Offload heavy logic to the Engine or EventBus.
- **Packet Mutability**: Treat received packets as immutable. Create new packets for modification/injection.
