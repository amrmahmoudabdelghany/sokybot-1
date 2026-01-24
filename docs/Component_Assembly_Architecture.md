# Component Assembly Architecture - Context Factory Bundle

## Purpose

The `sokybot-context-factory` bundle is responsible for:
1. **Assembling components** from different bundles into groups and machines
2. **Publishing lifecycle events** via EventAdmin (replacing listener pattern)
3. **Enabling plugin awareness** - third-party plugins react to context lifecycle events

## Architecture Overview

### Component Assembly Pattern

```
┌─────────────────────────────────────────────────────────────┐
│          Component Sources (OSGi Services)                  │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │ IGamePersistence │  │ IProxyConnection │               │
│  │ Factory          │  │ Factory          │               │
│  └──────────────────┘  └──────────────────┘               │
│                                                              │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │ IEngineFactory   │  │ UI Services      │               │
│  │ (NEW - needed)   │  │ (from UI bundle) │               │
│  └──────────────────┘  └──────────────────┘               │
│                                                              │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               │ Used by
                               │
┌──────────────────────────────▼───────────────────────────────┐
│          sokybot-context-factory Bundle                      │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │ ContextAssembler                                   │    │
│  │  - Assembles Group Components                      │    │
│  │  - Assembles Machine Components                    │    │
│  │  - Publishes events via EventAdmin                 │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌──────────────────┐         ┌──────────────────┐         │
│  │ GroupAssembler   │         │ MachineAssembler │         │
│  │  - Persistence   │         │  - Proxy         │         │
│  │  - UI            │         │  - Engine        │         │
│  │  - Machines      │         │  - UI            │         │
│  └──────────────────┘         └──────────────────┘         │
│                                                              │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               │ Publishes Events
                               │
┌──────────────────────────────▼───────────────────────────────┐
│          EventAdmin (OSGi)                                   │
│  Events: GROUP_CONTEXT_CREATED/DESTROYED                     │
│          MACHINE_CONTEXT_CREATED/DESTROYED                   │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               │ Consumed by
                               │
┌──────────────────────────────▼───────────────────────────────┐
│          Third-Party Plugins                                 │
│  - Listen to lifecycle events                                │
│  - Install themselves into contexts                          │
│  - React to context creation/destruction                     │
└──────────────────────────────────────────────────────────────┘
```

## Component Structure

### Group Components

A group consists of:
1. **Persistence Interface** (`IGamePersistenceFactory`)
   - Shared by all machines in the group
   - Created once per group
   - Accessed via `IGroupContext.getGameDAO()`

2. **UI Components** (from machine-ui bundle)
   - Group-level pages/dashboards
   - Navigation tree entries
   - Managed by machine-ui bundle (reacts to events)

3. **Child Machines** (collection of `IMachineContext`)
   - Each machine assembled separately
   - Machines share group's persistence

### Machine Components

A machine consists of:
1. **IProxyConnection** (from proxy bundle)
   - Network connection for this machine
   - Created via `IProxyConnectionFactory.createConnection(machineId, listener)`
   - Provides `IPacketPublisher`

2. **Engine Instance** (from engine bundle)
   - **NEW**: Public interface `IEngine` and factory `IEngineFactory` ✅ **Created**
   - Each machine has its own engine instance
   - Contains: State machine (internal), actuators, controllers
   - Created via `IEngineFactory.createEngine(machineId, proxy, groupName, machineName)`
   - **Note**: IPacketPublisher accessed via `IMachineContext.packetPublisher()`, not from engine

3. **UI Components** (from machine-ui bundle)
   - Machine-specific pages (Training, Environment, Logs, etc.)
   - Machine dashboard
   - Managed by machine-ui bundle (reacts to events)

4. **Other Components** (future extensibility)
   - Plugin services
   - Custom components
   - Added by third-party bundles via events

## Required Interfaces

### IEngine Interface (NEW - needed in sokybot-api)

```java
package org.sokybot.engine;

/**
 * Public interface for a machine engine instance.
 * Each machine has its own engine instance.
 * 
 * Note: This interface is framework-agnostic and does not expose
 * Spring-specific types or implementation details.
 * 
 * - IPacketPublisher is accessed via IMachineContext.packetPublisher()
 * - State machine access (if needed by plugins) should be through IMachineContext
 */
public interface IEngine {
    
    /**
     * Gets the machine ID this engine belongs to.
     * Format: "groupName.machineName"
     */
    String getMachineId();
    
    /**
     * Starts the engine (state machine, actuators, etc.).
     */
    void start();
    
    /**
     * Stops the engine and releases resources.
     */
    void stop();
    
    /**
     * Checks if the engine is currently running.
     */
    boolean isRunning();
}
```

**Design Decisions:**
- ❌ No `getStateMachine()` - API shouldn't depend on Spring StateMachine
- ❌ No `getPacketPublisher()` - Access via `IMachineContext.packetPublisher()` instead
- ✅ Minimal interface - only lifecycle methods
- ✅ Framework-agnostic - no Spring dependencies

### IEngineFactory Interface (NEW - needed in sokybot-api)

```java
package org.sokybot.engine;

import org.sokybot.proxy.IProxyConnection;

/**
 * Factory for creating engine instances for machines.
 * Similar to IProxyConnectionFactory.
 */
public interface IEngineFactory {
    
    /**
     * Creates an engine instance for a machine.
     * 
     * @param machineId The full machine ID (groupName.machineName)
     * @param proxyConnection The proxy connection for this machine
     * @param config Machine configuration (settings, etc.)
     * @return The created engine instance
     */
    IEngine createEngine(String machineId, IProxyConnection proxyConnection, MachineConfig config);
    
    /**
     * Destroys an engine instance.
     * 
     * @param machineId The machine ID
     */
    void destroyEngine(String machineId);
}
```

## Factory Bundle Implementation

### ContextAssembler

```java
@Component(immediate = true)
public class ContextAssembler {
    
    @Reference
    private EventAdmin eventAdmin;
    
    @Reference
    private IGamePersistenceFactory persistenceFactory;
    
    @Reference
    private IProxyConnectionFactory proxyFactory;
    
    @Reference
    private IEngineFactory engineFactory;
    
    /**
     * Assembles a group context.
     */
    public IGroupContext assembleGroup(GroupInfo groupInfo) {
        // 1. Create persistence (shared by all machines in group)
        IGamePersistenceFactory persistence = persistenceFactory.createForGroup(groupInfo);
        
        // 2. Create group context
        IGroupContext groupContext = new GroupContextImpl(groupInfo, persistence);
        
        // 3. Publish event
        publishGroupCreated(groupContext);
        
        return groupContext;
    }
    
    /**
     * Assembles a machine context.
     */
    public IMachineContext assembleMachine(IGroupContext groupContext, MachineInfo machineInfo) {
        String machineId = machineInfo.getGroup().getName() + "." + machineInfo.getMachineName();
        
        // 1. Create proxy connection
        IProxyConnection proxy = proxyFactory.createConnection(machineId, connectionListener);
        
        // 2. Create engine instance (engine gets packet publisher from proxy internally)
        IEngine engine = engineFactory.createEngine(machineId, proxy, 
            machineInfo.getGroup().getName(), machineInfo.getMachineName());
        
        // 3. Create machine context (wraps proxy, engine, etc.)
        IMachineContext machineContext = new MachineContextImpl(machineInfo, proxy, engine);
        // IMachineContext provides:
        //   - packetPublisher() - from proxy connection
        //   - getEngine() - if needed by plugins (returns IEngine)
        
        // 4. Publish event (triggers UI creation, plugin installation)
        publishMachineCreated(machineContext);
        
        return machineContext;
    }
    
    private void publishGroupCreated(IGroupContext context) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ContextLifecycleEvents.PROP_GROUP_NAME, context.name());
        properties.put(ContextLifecycleEvents.PROP_CONTEXT, context);
        properties.put(ContextLifecycleEvents.PROP_TIMESTAMP, System.currentTimeMillis());
        
        Event event = new Event(ContextLifecycleEvents.TOPIC_GROUP_CONTEXT_CREATED, properties);
        eventAdmin.postEvent(event);
    }
    
    private void publishMachineCreated(IMachineContext context) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ContextLifecycleEvents.PROP_GROUP_NAME, context.getGroupName());
        properties.put(ContextLifecycleEvents.PROP_MACHINE_NAME, context.name());
        properties.put(ContextLifecycleEvents.PROP_FULL_NAME, context.fullName());
        properties.put(ContextLifecycleEvents.PROP_CONTEXT, context);
        properties.put(ContextLifecycleEvents.PROP_TIMESTAMP, System.currentTimeMillis());
        
        Event event = new Event(ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED, properties);
        eventAdmin.postEvent(event);
    }
}
```

## Event-Driven Plugin Installation

### Plugin Example

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
        IMachineContext context = (IMachineContext) event.getProperty(ContextLifecycleEvents.PROP_CONTEXT);
        String machineId = (String) event.getProperty(ContextLifecycleEvents.PROP_FULL_NAME);
        
        // Plugin installs itself into the machine context
        installPlugin(context, machineId);
    }
    
    private void installPlugin(IMachineContext context, String machineId) {
        // Get packet publisher from context (not from engine)
        IPacketPublisher packetPublisher = context.packetPublisher();
        
        // Get engine if needed (for lifecycle operations only)
        // Note: State machine access would require framework-specific code
        // Plugins should work through events and context services, not direct engine access
        IEngine engine = context.getEngine(); // If IMachineContext exposes this
        
        // Subscribe to game events via EventAdmin
        // Register UI components via IMachinePageViewer
        // Access services via context
        // etc.
    }
}
```

## Migration from Listeners to Events

### Current (Listener Pattern)
```java
// Old way - using listeners
context.addGroupListener(listener);
context.addMachineListener(listener);
```

### New (EventAdmin)
```java
// New way - using EventAdmin
@Component(
    service = EventHandler.class,
    property = {
        EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED
    }
)
public class MyComponent implements EventHandler {
    @Override
    public void handleEvent(Event event) {
        // React to event
    }
}
```

**Benefits**:
- ✅ Decoupled - no direct dependency on contexts
- ✅ Multiple handlers can react to same event
- ✅ Standard OSGi pattern
- ✅ Plugins can install themselves reactively

## Implementation Tasks

1. **Create IEngine and IEngineFactory interfaces** in `sokybot-api`
2. **Implement IEngineFactory in engine bundle** (exposes engine creation)
3. **Create ContextAssembler in factory bundle**
4. **Replace listener pattern with EventAdmin events**
5. **Update machine-ui bundle** to react to events (already done)
6. **Document plugin development** with event-driven pattern
