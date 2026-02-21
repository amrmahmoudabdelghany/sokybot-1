---
name: Diagnostics & Debugging
description: How to use the 'soky' CLI and Karaf 'dev-shell' for diagnostics and debugging.
---

# Diagnostics & Debugging

Sokybot provides a unified `soky` CLI wrapper and a rich custom Karaf shell module (`sokybot-dev-shell`) to inspect the running system without needing full backend restarts.

## 1. The `soky` CLI

Use the `soky` CLI from any directory to manage the global state of the development environment.

```bash
soky status                  # Check if backend and frontend servers are running
soky backend shell           # Attach to the running Karaf backend console
soky backend logs [filter]   # Tail backend logs, optionally filtered
soky backend restart --fresh # Restart backend and wipe the OSGi cache
soky ui dev                  # Start frontend dev servers
soky kill                    # Force kill all processes if stuck
soky deploy <module>         # Compile, hot-reload, and clear cache for a module
```

## 2. Karaf Dev-Shell Commands

Once attached to the backend (`soky backend shell`), use the `dev:*` namespace commands to manage and debug the bot state in real time.

### A. System & Topology
- `dev:status` : Overview of the engine, groups, and bot counts.
- `dev:group-list` : List all loaded groups.
- `dev:bot-list <group>` : List all bots in a specific group along with their status.

### B. Bot Lifecycle
Manage bot instances without using the Web UI.
- `dev:bot-start <group> <bot>` : Start the engine for a bot.
- `dev:bot-stop <group> <bot>` : Stop the engine for a bot.
- `dev:bot-restart <group> <bot>` : Restart the engine for a bot (useful for recovering stuck state).

### C. Advanced Debugging

These are your primary tools when developing bot logic, bypassing the need for tedious UI clicks or full recompilations.

**1. State Inspection**
Dump the current known state of a bot's trainer entity (HP, MP, Position, Target):
```bash
karaf@root()> dev:inspect <group> <bot>
```

**2. Groovy Evaluation**
Run arbitrary Groovy code against a bot's context in real time. Very useful for testing logic queries before committing them to an actuator script.
```bash
karaf@root()> dev:eval <group> <bot> "context.getService(org.sokybot.gamemodel.IGameModel.class).observeAll().blockFirst()"
```

**3. Packet Injection**
Inject an arbitrary hex packet into the network stream to trigger your packet handlers or deceive the bot state.
```bash
# Inject as if coming from Server (simulates receiving data)
karaf@root()> dev:inject <group> <bot> "30D2 00 01 02"

# Inject as if coming from Client (simulates sending data)
karaf@root()> dev:inject <group> <bot> "30D2 00 01 02" --client
```

### D. Script Management
When editing `.groovy` scripts or `.json` UI schemas in `scripts/`, you can force the system to reload them globally.
- `dev:script-list` : View all loaded script actuators and pages.
- `dev:script-reload` : Unload and recompile all scripts immediately.
