# Component Assembly Implementation Plan

## Overview

The `sokybot-context-factory` bundle assembles components from different bundles into groups and machines, and publishes lifecycle events via EventAdmin for plugin awareness.

## Component Structure

### Group Components
- **Persistence Interface** (`IGamePersistenceFactory`) - shared by all machines
- **UI Components** - from machine-ui bundle (reacts to events)
- **Child Machines** - collection of assembled machines

### Machine Components
- **IProxyConnection** - from proxy bundle (via `IProxyConnectionFactory`)
- **Engine Instance** - from engine bundle (via `IEngineFactory` - **NEW**)
- **UI Components** - from machine-ui bundle (reacts to events)
- **Other Components** - plugins installed via events

## Implementation Steps

### Step 1: Create Engine Public Interface

**Files to create:**
1. `sokybot-api/src/main/java/org/sokybot/engine/IEngine.java` ✅ **Created**
2. `sokybot-api/src/main/java/org/sokybot/engine/IEngineFactory.java` ✅ **Created**

**Purpose:**
- Expose engine functionality without implementation details
- Framework-agnostic (no Spring dependencies)
- Similar to `IProxyConnection` and `IProxyConnectionFactory`
- Allows factory bundle to create engines

**Design Decisions:**
- ❌ No `StateMachine` exposure - API shouldn't depend on Spring
- ❌ No `IPacketPublisher` exposure - Access via `IMachineContext.packetPublisher()`
- ✅ Minimal interface - only lifecycle methods (`start()`, `stop()`, `isRunning()`, `getMachineId()`)

### Step 2: Implement IEngineFactory in Engine Bundle

**Location:** `sokybot-engine/src/main/java/org/sokybot/engine/EngineFactory.java`

**Responsibilities:**
- Creates Spring contexts with `MachineConfig` for each machine
- Wraps Spring context as `IEngine` implementation
- Manages engine lifecycle
- Registers as OSGi service

**Example:**
```java
@Component(service = IEngineFactory.class)
public class EngineFactory implements IEngineFactory {
    
    private final Map<String, IEngine> engines = new ConcurrentHashMap<>();
    
    @Override
    public IEngine createEngine(String machineId, IProxyConnection proxyConnection,
                               String groupName, String machineName) {
        // Create Spring context with MachineConfig
        // Wrap as IEngine implementation
        // Store and return
    }
}
```

### Step 3: Create Context Factory Bundle

**Bundle:** `sokybot-context-factory`

**Dependencies:**
- `sokybot-api`
- `sokybot-proxy` (for IProxyConnectionFactory)
- `sokybot-engine` (for IEngineFactory)
- `sokybot-persistence` (for IGamePersistenceFactory)
- OSGi Event Admin
- OSGi Declarative Services

### Step 4: Implement ContextAssembler

**Component:** `ContextAssembler`

**Responsibilities:**
1. Assemble group components:
   - Create persistence via `IGamePersistenceFactory`
   - Create group context
   - Publish `GROUP_CONTEXT_CREATED` event

2. Assemble machine components:
   - Create proxy via `IProxyConnectionFactory`
   - Create engine via `IEngineFactory`
   - Create machine context
   - Publish `MACHINE_CONTEXT_CREATED` event

**Key Methods:**
- `assembleGroup(GroupInfo)` → `IGroupContext`
- `assembleMachine(IGroupContext, MachineInfo)` → `IMachineContext`

### Step 5: Replace Listener Pattern with EventAdmin

**Current:** Uses `IGroupListener` and `IMachineListener`
**New:** Uses EventAdmin events only

**Migration:**
- Factory publishes events via EventAdmin
- All listeners migrate to EventHandler components
- Deprecate listener interfaces over time

### Step 6: Update Machine UI Bundle

**Already done:** Machine UI bundle listens to events
- No changes needed (already uses EventHandler)
- Will automatically react to factory-published events

## Event Flow

```
1. ContextAssembler.assembleGroup()
   ├── Creates persistence
   ├── Creates group context
   └── Publishes GROUP_CONTEXT_CREATED event
       ├── Machine UI bundle reacts → Creates group UI
       └── Plugins react → Install into group

2. ContextAssembler.assembleMachine()
   ├── Creates proxy connection
   ├── Creates engine instance
   ├── Creates machine context
   └── Publishes MACHINE_CONTEXT_CREATED event
       ├── Machine UI bundle reacts → Creates machine UI
       ├── Game events bundle connects → Subscribes to packets
       └── Plugins react → Install into machine
```

## Plugin Development Pattern

Plugins use EventHandler to react to context lifecycle:

```java
@Component(
    service = EventHandler.class,
    property = {
        EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED
    }
)
public class MyPlugin implements EventHandler {
    
    @Override
    public void handleEvent(Event event) {
        IMachineContext context = (IMachineContext) 
            event.getProperty(ContextLifecycleEvents.PROP_CONTEXT);
        IEngine engine = context.getEngine();
        
        // Plugin installs itself
        engine.getStateMachine().addStateListener(...);
        // etc.
    }
}
```

## Benefits

1. **Clear Component Assembly**: Explicit assembly of components
2. **Plugin Awareness**: Event-driven plugin installation
3. **Decoupling**: Components don't depend on each other
4. **Standard Pattern**: EventAdmin is standard OSGi
5. **Extensibility**: Easy to add new components
6. **Testability**: Components can be mocked easily
