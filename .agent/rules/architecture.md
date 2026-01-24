---
trigger: always_on
glob:
description: Project architecture and module overview
---

# Sokybot Architecture

## 1. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Electron Desktop Shell                   │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Web UI (React/TypeScript)                 │  │
│  │         sokybot-webview / sokybot-dev-tools           │  │
│  └──────────────────────┬────────────────────────────────┘  │
│                         │ HTTP / WebSocket                   │
│  ┌──────────────────────▼────────────────────────────────┐  │
│  │              sokybot-http-server (Vert.x)              │  │
│  └──────────────────────┬────────────────────────────────┘  │
│                         │                                    │
│  ┌──────────────────────▼────────────────────────────────┐  │
│  │                 Apache Karaf (OSGi)                    │  │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐  │  │
│  │  │   Engine    │ │    Proxy    │ │   Persistence   │  │  │
│  │  │ (Bot Logic) │ │  (Network)  │ │   (Database)    │  │  │
│  │  └─────────────┘ └─────────────┘ └─────────────────┘  │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 2. Module Categories

### Core Engine
| Module | Responsibility |
|--------|----------------|
| `sokybot-engine` | Main bot logic, state machines |
| `sokybot-engine-api` | Engine interfaces/contracts |
| `sokybot-runtime` | Context management, lifecycle |

### Network Layer
| Module | Responsibility |
|--------|----------------|
| `sokybot-proxy` | Game client ↔ server proxy (Netty) |
| `sokybot-security` | Blowfish encryption, packet security |
| `sokybot-packet-sniffer` | Packet capture and analysis |

### Game Data
| Module | Responsibility |
|--------|----------------|
| `sokybot-pk2` | PK2 archive format reader |
| `sokybot-pk2-extractor` | Game data extraction |
| `sokybot-game-loader` | Game client data loading |
| `sokybot-game-model` | Game entity models |
| `sokybot-game-asset` | Asset management |
| `sokybot-game-events` | Packet event definitions |
| `sokybot-game-navigation` | Pathfinding, navigation mesh |

### UI Layer
| Module | Responsibility |
|--------|----------------|
| `sokybot-http-server` | HTTP/WebSocket API (Vert.x) |
| `sokybot-webview` | Hybrid Web UI (Java Backend + React Frontend) |
| `sokybot-dev-tools` | Developer tools UI |
| `sokybot-swing` | Swing UI components |

### Infrastructure
| Module | Responsibility |
|--------|----------------|
| `sokybot-commons` | Shared utilities |
| `sokybot-persistence` | Database (Hibernate/H2) |
| `sokybot-settings` | User settings/profiles |
| `sokybot-features` | Karaf feature definitions |
| `sokybot-dist` | Distribution assembly |

## 3. Communication Patterns

### Frontend ↔ Backend
- **Protocol**: HTTP REST + WebSocket
- **Port**: Configured in Karaf (default 8181)
- **Format**: JSON

### Inter-Bundle Communication
- **Pattern**: OSGi Services (Declarative Services)
- **Discovery**: Service Registry
- **Lifecycle**: Managed by SCR (Service Component Runtime)

### Game Communication
- **Protocol**: Custom binary protocol over TCP
- **Encryption**: Blowfish
- **Pattern**: Proxy intercepts client ↔ server traffic

## 4. Key Interfaces

| Interface | Location | Purpose |
|-----------|----------|---------|
| `IGameLoader` | `sokybot-loader-api` | Game data loading contract |
| `IRuteFinder` | `sokybot-game-navigation` | Pathfinding contract |
| `ISokybotContext` | `sokybot-runtime` | Application context |
| `IMachineContext` | `sokybot-runtime` | Bot instance context |

## 5. Data Flow

```
Game Client ──► Proxy ──► Packet Handler ──► Event Bus ──► Engine
                 │                                           │
                 ▼                                           ▼
            Packet Sniffer                              State Machine
                 │                                           │
                 ▼                                           ▼
            WebSocket ◄──────────────────────────────► HTTP API
                 │                                           │
                 ▼                                           ▼
              Web UI ◄──────────────────────────────► Web UI
```

## 6. Build Order (Module Dependencies)

Build proceeds roughly in this order:
1. `sokybot-commons`, `sokybot-security`
2. `sokybot-pk2`, `sokybot-loader-api`
3. `sokybot-game-*` modules
4. `sokybot-engine-api`, `sokybot-runtime`
5. `sokybot-engine`, `sokybot-proxy`
6. `sokybot-http-server`
7. `sokybot-webview`, `sokybot-dev-tools`
8. `sokybot-features`, `sokybot-dist`
