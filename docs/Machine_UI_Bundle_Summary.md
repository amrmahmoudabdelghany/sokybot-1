# Machine UI Bundle - Implementation Summary

## Overview

Created a dedicated `sokybot-machine-ui` bundle that uses OSGi Event Admin to reactively create and destroy machine UI components based on context lifecycle events.

## Architecture

### Event-Driven UI Creation

```
Engine (sokybot-engine)
  ├── GroupContextAdapter creates → publishes GROUP_CONTEXT_CREATED event
  ├── MachineContextAdapter creates → publishes MACHINE_CONTEXT_CREATED event
  └── Contexts destroyed → publish DESTROYED events

OSGi Event Admin
  └── Routes events to registered handlers

Machine UI Bundle (sokybot-machine-ui)
  └── MachineUIEventHandler listens to events
      ├── On MACHINE_CONTEXT_CREATED → creates MachineUIInstance
      ├── On MACHINE_CONTEXT_DESTROYED → destroys MachineUIInstance
      ├── On GROUP_CONTEXT_CREATED → creates GroupUIInstance
      └── On GROUP_CONTEXT_DESTROYED → destroys GroupUIInstance
```

## Files Created

### 1. API Layer (`sokybot-api`)

**`ContextLifecycleEvents.java`**
- Defines event topics and property names
- Event topics:
  - `sokybot/context/group/CREATED`
  - `sokybot/context/group/DESTROYED`
  - `sokybot/context/machine/CREATED`
  - `sokybot/context/machine/DESTROYED`
- Event properties:
  - `groupName` (String)
  - `machineName` (String)
  - `fullName` (String - "groupName.machineName")
  - `context` (IGroupContext or IMachineContext)
  - `timestamp` (Long)

### 2. Machine UI Bundle (`sokybot-machine-ui`)

**Bundle Structure:**
```
sokybot-machine-ui/
├── pom.xml
└── src/main/java/org/sokybot/machineui/
    ├── MachineUIActivator.java
    ├── MachineUIEventHandler.java
    ├── MachineUIInstance.java
    └── GroupUIInstance.java
```

**Components:**

1. **`MachineUIActivator.java`**
   - Simple bundle activator
   - Logs bundle start/stop

2. **`MachineUIEventHandler.java`**
   - OSGi Declarative Services component
   - Implements `EventHandler` interface
   - Listens to all context lifecycle events
   - Manages `MachineUIInstance` and `GroupUIInstance` objects
   - Uses `@Reference` to inject:
     - `PageContainer`
     - `DashboardContainer`
     - `INavTree`

3. **`MachineUIInstance.java`**
   - Manages UI for one machine
   - Creates pages and dashboard when `createPages()` called
   - Removes all UI components when `destroyPages()` called
   - Tracks registered pages for cleanup

4. **`GroupUIInstance.java`**
   - Manages UI for one group
   - Currently minimal (placeholder for future group-level UI)

## Next Steps

### 1. Publish Events from Engine

**Update `GroupContextAdapter.java`:**
```java
@PostConstruct
private void publishCreated() {
    if (eventAdmin != null) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ContextLifecycleEvents.PROP_GROUP_NAME, this.groupInfo.getName());
        properties.put(ContextLifecycleEvents.PROP_CONTEXT, this);
        properties.put(ContextLifecycleEvents.PROP_TIMESTAMP, System.currentTimeMillis());
        
        Event event = new Event(ContextLifecycleEvents.TOPIC_GROUP_CONTEXT_CREATED, properties);
        eventAdmin.postEvent(event);
    }
}

@PreDestroy
private void publishDestroyed() {
    if (eventAdmin != null) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ContextLifecycleEvents.PROP_GROUP_NAME, this.groupInfo.getName());
        properties.put(ContextLifecycleEvents.PROP_TIMESTAMP, System.currentTimeMillis());
        
        Event event = new Event(ContextLifecycleEvents.TOPIC_GROUP_CONTEXT_DESTROYED, properties);
        eventAdmin.postEvent(event);
    }
}
```

**Update `MachineContextAdapter.java`:**
```java
// In constructor or after Spring context is ready
private void publishCreated() {
    if (eventAdmin != null) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ContextLifecycleEvents.PROP_GROUP_NAME, machineInfo.getGroup().getName());
        properties.put(ContextLifecycleEvents.PROP_MACHINE_NAME, machineInfo.getMachineName());
        properties.put(ContextLifecycleEvents.PROP_FULL_NAME, fullName());
        properties.put(ContextLifecycleEvents.PROP_CONTEXT, this);
        properties.put(ContextLifecycleEvents.PROP_TIMESTAMP, System.currentTimeMillis());
        
        Event event = new Event(ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED, properties);
        eventAdmin.postEvent(event);
    }
}
```

**Add EventAdmin to EngineConfig:**
```java
@Bean
public EventAdmin eventAdmin(BundleContext bundleContext) {
    if (bundleContext == null) return null;
    ServiceReference<EventAdmin> ref = bundleContext.getServiceReference(EventAdmin.class);
    return ref != null ? bundleContext.getService(ref) : null;
}
```

### 2. Implement Page Factories

Create factory services for each page type:
- `TrainingPageFactory` - creates Training page
- `EnvironmentPageFactory` - creates Environment page
- `LogPageFactory` - creates Logs page
- `HuntingPageFactory` - creates Hunting page
- `DashboardFactory` - creates machine dashboard

Each factory should:
- Be an OSGi service
- Implement a factory interface
- Accept `IMachineContext` as parameter
- Return UI component

### 3. Move UI Components from Engine

- Move `machine/page/` directory to `sokybot-machine-ui`
- Move `machine/dashboard/` to `sokybot-machine-ui`
- Update imports
- Remove UI dependencies from engine `pom.xml`

### 4. Testing

Test scenarios:
1. Create a group → verify GROUP_CONTEXT_CREATED event → verify group UI created
2. Create a machine → verify MACHINE_CONTEXT_CREATED event → verify machine UI created
3. Destroy a machine → verify MACHINE_CONTEXT_DESTROYED event → verify UI removed
4. Destroy a group → verify GROUP_CONTEXT_DESTROYED event → verify group UI removed
5. Create multiple machines → verify each gets its own UI
6. Test UI isolation between machines

## Benefits

1. **Decoupled**: Engine doesn't know about UI bundle
2. **Reactive**: UI automatically appears/disappears with contexts
3. **Event-Driven**: Clean OSGi event-based architecture
4. **Extensible**: Easy to add new page factories
5. **Testable**: Can test event publishing/handling separately

## Dependencies

- `sokybot-api` - for interfaces and event constants
- `sokybot-ui` - for shared UI infrastructure (PageContainer, etc.)
- `sokybot-swing` - for UI components
- OSGi Event Admin - for event handling
- OSGi Declarative Services - for component management

## Module Registration

Added `sokybot-machine-ui` to parent `pom.xml` modules list.
