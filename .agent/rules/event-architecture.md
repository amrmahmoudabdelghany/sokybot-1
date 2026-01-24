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

## 3. Event Handling (Subscribers)

To subscribe to events, register an `EventHandler` service:

```java
@Component(
    property = {
        EventConstants.EVENT_TOPIC + "=sokybot/game/*" // Wildcard subscription
    }
)
public class MyEventHandler implements EventHandler {

    @Override
    public void handleEvent(Event event) {
        Object pojo = event.getProperty("event");
        if (pojo instanceof MySpecificEvent) {
            // Handle event
        }
    }
}
```

## 4. Best Practices
1. **Async by Default**: Assume event delivery is asynchronous.
2. **No Return Values**: Events are fire-and-forget. Use Request-Response patterns (OSGi Services) if you need a result.
3. **Filtering**: Use `EventConstants.EVENT_FILTER` in `@Component` properties to filter events at the OSGi framework level efficiently.
