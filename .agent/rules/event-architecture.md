---
trigger: always_on
glob: "**/*.java"
description: Event-Driven Architecture patterns using OSGi EventAdmin
---

# Event-Driven Architecture Rules

## 1. Core Principle
Decouple components using OSGi `EventAdmin`. Components should publish events rather than calling dependent services directly for notifications.

## 2. Event Publishing

### Publisher Mechanism
- Use `org.osgi.service.event.EventAdmin` service.
- **Do not** implement custom event buses unless strictly necessary for internal module logic.

### Topic Convention
Topics MUST follow the hierarchical structure:

`sokybot/game/{machineId}/{EventType}`

- `sokybot/game/` - Root prefix
- `{machineId}` - Unique identifier (Group.MachineName)
- `{EventType}` - Simple class name of the event (e.g., `SkillCastEvent`, `PacketReceivedEvent`)

### Payload Format
The OSGi `Event` properties map must contain:
1. `event`: The actual POJO event object.
2. `timestamp`: Event creation time.
3. `machineName`: Name of the source machine.
4. `groupName`: Name of the source group.

## 3. High-Level Event Handling (Reactive)

For typed event streams and loose coupling at the component level, use `IReactiveEventBus`. This bus bridges OSGi `EventAdmin` to Project Reactor `Flux`.

### Usage
Inject `IReactiveEventBus` via DS `@Reference`:

```java
@Reference
private IReactiveEventBus eventBus;

public void start() {
    // Subscribe to specific event types
    eventBus.on(MonsterSpawnEvent.class)
            .subscribe(this::handleMonsterSpawn);
}
```

## 4. Topic Convention
Topics MUST follow the hierarchical structure:

`sokybot/game/{machineId}/{EventType}`

- `sokybot/game/` - Root prefix
- `{machineId}` - Unique identifier (Group.MachineName)
- `{EventType}` - Simple class name of the event (e.g., `SkillCastEvent`, `PacketReceivedEvent`)

## 5. Payload Format
The OSGi `Event` properties map must contain:
1. `event`: The actual POJO event object.
2. `timestamp`: Event creation time.
3. `machineName`: Name of the source machine.
4. `groupName`: Name of the source group.

## 6. Best Practices
1. **Async by Default**: Assume event delivery is asynchronous.
2. **No Return Values**: Events are fire-and-forget. Use Request-Response patterns (OSGi Services) if you need a result.
3. **Filtering**: Use `EventConstants.EVENT_FILTER` in `@Component` properties or `Flux.filter()` in the reactive bus.
4. **Lifecycle**: Ensure subscriptions are disposed in `stop()` or `@Deactivate`.

## 7. Reactive State Stores

While the `IReactiveEventBus` is ideal for handling transient events (e.g., chat messages, level up), persistent state (e.g., character stats, monster locations) is better managed via the `IGameModel`.

### Core Principle
The `IGameModel` acts as a reactive state store that broadcasts consolidated updates.

### Usage
Observe state changes using Project Reactor `Flux`:

```java
@Reference
private IGameModel gameModel;

public void start() {
    // Observe updates for a specific entity type
    gameModel.observe(Character.class)
            .subscribe(update -> {
                Character c = update.getEntity();
                switch (update.getType()) {
                    case ADDED:   log.info("Spawned: {}", c.getName()); break;
                    case UPDATED: log.info("Updated: {}", c.getName()); break;
                    case REMOVED: log.info("Despawned: {}", c.getName()); break;
                }
            });
}
```

### Best Practices
- Use `observeAll()` for a unified stream of all entity changes.
- Actuators SHOULD prioritize observing the model over raw packet events to ensure stay in sync with the consolidated view.
