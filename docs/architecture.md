# Sokybot Architecture

This document provides a comprehensive technical overview of the Sokybot application architecture.

## 1. High-Level Overview

Sokybot is a modular, event-driven botting platform for Silkroad Online, built on the **Apache Karaf** OSGi runtime. It employs a **Game Proxy** architecture, positioning itself between the game client and the server to intercept, analyze, and automate game traffic.

### Architectural Layers

```mermaid
graph TD
    subgraph Frontend [Electron / Web Browser]
        UI[React Web UI]
        DevTools[Developer Tools]
    end

    subgraph Backend [OSGi Container - Apache Karaf]
        API[HTTP / WebSocket Layer]
        Engine[Bot Engine / Logic]
        Network[Network Proxy Layer]
        Data[Game Data / Persistence]
    end

    subgraph External
        Client[Game Client]
        Server[Game Server]
    end

    UI <-->|RSocket/WS| API
    DevTools <-->|RSocket/WS| API
    Client <-->|TCP (Silkroad Protocol)| Network
    Network <-->|TCP (Silkroad Protocol)| Server
    Network -->|Events| Engine
    Engine -->|Commands| Network
    Engine -->|State Updates| API
    Data -->|Models| Engine
```

## 2. Technology Stack

### Backend (Java 11)
*   **Runtime**: Apache Karaf (OSGi)
*   **Networking**: Netty 4.1 (Async Event-Driven)
*   **Reactive Programming**: Project Reactor (Mono/Flux)
*   **HTTP/WebSocket**: Reactor Netty (Shared HTTP Server)
*   **Protocol**: RSocket (Frontend communication)
*   **Persistence**: Hibernate (JPA), H2 Database (Embedded)
*   **Data Format**: JSON (Jackson), Custom Binary (Silkroad)

### Frontend (TypeScript)
*   **Framework**: React 19
*   **Build Tool**: Vite
*   **Styling**: TailwindCSS
*   **Components**: Radix UI Primitives, Lucide Icons

## 3. Module Breakdown

The codebase is organized into multi-module Maven projects, categorized by responsibility.

### 3.1 Core Modules (`core/`)
These modules form the brain of the application.

*   **`sokybot-runtime`**: Manages the lifecycle of bot instances (Machines) and groups. It consumes the `IEngineFactory` (provided by `sokybot-engine`) to create new bot contexts and handles the OSGi service wiring.
*   **`sokybot-engine`**: Implements the state machines and core logic loops (Cycles). It provides the `IEngineFactory` service and orchestrates Actuators discovered via OSGi.
*   **`sokybot-engine-api`**: Defines the public interfaces (`IDispatcher`, `ICycleDefinition`, `IWorkflowContext`, `IEngine`) to allow loose coupling between the engine and actuator implementations.

### 3.2 Network Layer (`network/`)
Handles all external tcp communication with the game.

*   **`sokybot-proxy`**: A Netty-based TCP proxy. It acts as a "Man-in-the-Middle" (MITM).
    *   **ClientToProxyConnection**: Handles traffic from the Game Client.
    *   **ProxyToServerConnection**: Handles traffic to the Game Server.
    *   It decrypts/encrypts packets on the fly and allows the Engine to inject or suppress packets.
*   **`sokybot-security`**: Implementation of Silkroad's security protocols.
    *   **Blowfish**: Packet encryption/decryption.
    *   **CRC/Count**: Integrity checks (Handshake).
*   **`sokybot-packet-sniffer`**: A development tool for capturing and analyzing packet streams in real-time.

### 3.3 Game Data (`game/`)
Manages game assets and protocol definitions.

*   **`sokybot-game-events`**: A large module containing definitions that map raw packet OpCodes (e.g., `0x3015`) to strongly-typed Java Events (e.g., `ChatMessageEvent`).
    *   **Translators**: Convert `ImmutablePacket` ↔ `GameEvent`.
*   **`sokybot-game-events-api`**: Public interfaces for the event translation system.
*   **`sokybot-pk2`**: A driver for reading `.pk2` files (Silkroad's custom archive format).
*   **`sokybot-pk2-extractor`**: Extracts and processes data from PK2 archives.
*   **`sokybot-game-model`**: POJO domain models for game entities (Character, Item, Skill, Mob).
*   **`sokybot-game-navigation`**: Pathfinding logic and navigation mesh handling.
*   **`sokybot-loader-api`**: Interfaces for game data loading (`IGameLoader`).
*   **`sokybot-game-loader`**: Handles launching and injecting into the game client process (shellcode injection, process I/O).
*   **`sokybot-game-asset`**: Asset management and caching.

### 3.4 User Interface (`ui/`)
The interface layer, decoupled from the backend.

*   **`sokybot-http-server`**: A Reactor Netty-based OSGi bundle that provides a shared HTTP host (default port 8182). It serves the static frontend assets and upgrades WebSocket connections for RSocket.
*   **`sokybot-webview`**: The main user interface application.
*   **`sokybot-dev-tools`**: Development tools UI for OSGi bundle management, service inspection, and runtime metrics.
*   **`sokybot-frontend-shared`**: Common UI components and utilities shared between webview and dev-tools.

### 3.5 Infrastructure (`infra/`)
Shared services and deployment configuration.

*   **`sokybot-commons`**: Utility classes (Hexdump, Byte manipulation, String helpers).
*   **`sokybot-persistence`**: JPA/Hibernate layer for storing extracted game data (Items, Skills, NPCs, Shops, Navigation Meshes) in an embedded H2 database.
*   **`sokybot-settings`**: User settings and profile management.
*   **`sokybot-features`**: Defines Karaf Features (groups of bundles) for easy provisioning.
*   **`sokybot-dist`**: Assembles the custom Karaf distribution, stripping out unnecessary enterprise features to keep the footprint small for desktop use.
*   **`sokybot-build-tools`**: Build configuration (Checkstyle rules, version rules).

### 3.6 Actuators (`actuators/`)
Pluggable bot logic modules, discovered via OSGi at runtime.

*   **`sokybot-actuator-connector`**: Handles initial connection and server selection.
*   **`sokybot-actuator-login`**: Automates the login sequence.
*   **`sokybot-actuator-training`**: Implements combat training loops.

## 4. Design Patterns & Principles

### 4.1 Event-Driven Architecture
The system is heavily event-driven, with clear separation of concerns:
1.  **Network Event**: A packet arrives at the Proxy.
2.  **Translation**: It is converted to a `GameEvent` by translators.
3.  **State Update**: `IGameModel` and `UI` subscribe to these events to update the game state (e.g., character HP, position).
4.  **Bot Logic**: The Engine and Actuators read the *state* from `IGameModel`. They do not consume events directly for state tracking.
5.  **Action**: Actuators use `IDispatcher` to send generated packets (commands) to the Client or Server.

### 4.2 Legacy Event-Driven Architecture (Deprecated)
In earlier versions, the logic flow was strictly event-based without a centralized state model:
1.  **Network Event** → **Translation** → **Event Bus**.
2.  **State Tracking**: The Engine consumed events specifically to track state internally.
3.  **Action**: Actuators consumed events directly to trigger actions (e.g., "On HP Update Event -> If HP < 50% -> Cast Heal").
*Note: This approach led to synchronization issues and has been replaced by the Model-based approach.*

### 4.3 Whiteboard Pattern
Used extensively with OSGi. Services (like `IRSocketHandler` or `IActuator`) register themselves with the OSGi registry. The consumer (e.g., `RSocketService` or `Engine`) listens for these registrations and dynamically adds them without hard dependencies.

### 4.4 Reactive Streams
Project Reactor (`Flux`, `Mono`) is used for asynchronous data flow, particularly in the Network and UI layers, to ensure non-blocking performance.

### 4.5 Logic Execution Model (Cycles)
The bot's intelligence is built on a **State Machine** pattern called "Cycles".
1.  **Actuators**: Logic is encapsulated in `Actuators` (implementing `IActuator`).
2.  **Cycles**: Each Actuator registers one or more `Cycles`. A Cycle is a directed graph of states (`CycleDefinition`).
3.  **Components**:
    *   **Guard**: A condition (`ctx -> boolean`) that determines if a state can be entered. If false, the cycle transitions to a fallback target.
    *   **Action**: Code to execute (`ctx -> void`) when in the state.
    *   **Context**: Access to `IGameModel` (for state) and `IDispatcher` (for actions) is provided via `IWorkflowContext`.
4.  **Flow**: The Engine evaluates active Cycles tick-by-tick. If a Cycle's entry guard is met, it becomes active and transitions through its states until completion or interruption.

## 5. Runtime View (Boot Process)

1.  **Karaf Startup**: The `sokybot-dist` assembly starts the Karaf container.
2.  **Feature Loading**: The `sokybot-full` feature is loaded (as defined in `etc/org.apache.karaf.features.cfg`).
3.  **Core Services**: `sokybot-runtime` and `sokybot-http-server` start.
4.  **UI Waiting**: The HTTP server opens port 8182.
5.  **User Action**: User creates a "Machine" via the UI.
6.  **Bot Launch**: `sokybot-runtime` uses `IEngineFactory` to instantiate a new Engine context, spins up a Netty proxy on a local port, and the user connects the Game Client to that local port.

## 6. Development & Build

*   **Build System**: Maven (via `mvnw` wrapper).
*   **frontend-maven-plugin**: Used to compile the React frontend during the Maven build phase.
*   **karaf-maven-plugin**: Used to verify features and assemble the distribution.

### Key Build Profiles
*   `dev`: Builds the code but skips heavy integration tests and optimizations.
*   `desktop`: optimized build for electron distribution.
