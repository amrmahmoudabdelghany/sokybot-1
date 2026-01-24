# Context Factory Bundle - Component Assembly Architecture

## Purpose

The `sokybot-context-factory` bundle:
1. **Assembles components** from different bundles into groups and machines
2. **Publishes lifecycle events** via EventAdmin (replaces listener pattern)
3. **Enables plugin awareness** - third-party plugins react to context lifecycle events

## Architecture Understanding

**Component Assembly Pattern**:
- `ISokybotContext` represents the **entire runtime** (single instance)
- Each `IGroupContext` consists of:
  - **Persistence interface** (shared by all machines)
  - **UI components** (from machine-ui bundle)
  - **Child machines** (collection of IMachineContext)
- Each `IMachineContext` consists of:
  - **IProxyConnection** (from proxy bundle, via factory)
  - **Engine instance** (from engine bundle, via **IEngineFactory** - NEW)
  - **UI components** (from machine-ui bundle)
  - **Other components** (plugins, etc.)

## Current State vs Target State

### Current Issues:
- Components created ad-hoc in adapters
- Listener pattern (IMachineListener, IGroupListener) - hard to extend
- No clear component assembly
- Engine not accessible via factory pattern

### Target State:
- Factory bundle **assembles** components explicitly
- EventAdmin events for lifecycle (replaces listeners)
- Clear component boundaries
- Engine accessible via `IEngineFactory` (like `IProxyConnectionFactory`)

## Proposed Solution: `sokybot-runtime` or `sokybot-context-factory` Bundle

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    OSGi Service Layer                        │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────┐         ┌──────────────────┐         │
│  │ ISokybotContext  │         │ EventAdmin       │         │
│  │  (from engine)   │         │                  │         │
│  └────────┬─────────┘         └────────┬─────────┘         │
│           │                             │                   │
│           │                             │                   │
└───────────┼─────────────────────────────┼───────────────────┘
            │                             │
            │                             │
┌───────────▼─────────────────────────────▼───────────────────┐
│          sokybot-context-factory Bundle                      │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  ContextFactoryManager                              │   │
│  │  - Monitors ISokybotContext                         │   │
│  │  - Creates GroupContextAdapter instances            │   │
│  │  - Creates MachineContextAdapter instances          │   │
│  │  - Publishes lifecycle events via EventAdmin        │   │
│  │  - Registers contexts as OSGi services              │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  GroupContextFactory                                │   │
│  │  - Factory for creating group contexts              │   │
│  │  - Publishes GROUP_CONTEXT_CREATED event            │   │
│  │  - Registers IGroupContext as OSGi service          │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  MachineContextFactory                              │   │
│  │  - Factory for creating machine contexts            │   │
│  │  - Publishes MACHINE_CONTEXT_CREATED event          │   │
│  │  - Registers IMachineContext as OSGi service        │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                               │
└─────────────────────────────────────────────────────────────┘
            │                             │
            │                             │
            │ Events                      │ Services
            │                             │
┌───────────▼─────────────────────────────▼───────────────────┐
│          sokybot-machine-ui Bundle                          │
│  - Listens to lifecycle events                              │
│  - Creates/destroys UI components                           │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│          sokybot-engine Bundle                               │
│  - Provides: ISokybotContext (entire runtime)               │
│  - Provides: Context adapter classes                        │
│  - Provides: MachineConfig (engine instance per machine)    │
│  - Responsibility: Machine workflows/logic                  │
│  - Each machine context = separate engine instance          │
│  - NO event publishing (factory handles this)               │
└─────────────────────────────────────────────────────────────┘
```

## Benefits

### 1. **Clear Separation of Concerns**
- **ISokybotContext (Engine)**: Entire runtime management (groups, machines)
- **Factory Bundle**: Event publishing layer (wraps ISokybotContext operations)
- **Machine Engine Instances**: Each machine has its own engine (Spring context with MachineConfig)
- **UI Bundle**: Reactive UI creation (listens to events)

### 2. **Single Responsibility**
- Factory bundle has ONE job: manage context lifecycle
- Engine has ONE job: provide machine workflows
- UI bundle has ONE job: display UI for contexts

### 3. **Loose Coupling**
- Engine doesn't know about Event Admin
- Engine doesn't know about OSGi services
- Factory is the only component that knows about both

### 4. **Testability**
- Engine can be tested without Event Admin
- Factory can be tested with mock Event Admin
- UI can be tested with mock events

### 5. **Extensibility**
- Other bundles can listen to lifecycle events
- Different UI implementations can be swapped
- Factory can add additional lifecycle management features

## Implementation Approach

### Option A: Factory Wraps ISokybotContext (Recommended)

**How it works:**
1. Engine provides `ISokybotContext` service (represents entire runtime)
2. Factory bundle wraps/intercepts `ISokybotContext` operations:
   - `installGroup()` → Delegate to engine → Publish `GROUP_CONTEXT_CREATED` event
   - Group creates machine via `installMachine()` → Factory intercepts → Publish `MACHINE_CONTEXT_CREATED` event
3. Factory publishes lifecycle events via Event Admin
4. Factory optionally registers contexts as OSGi services

**Key Understanding**:
- `ISokybotContext` = Entire runtime (single instance, manages all)
- Each `IMachineContext` = Has its own engine instance (Spring context with MachineConfig)
- Factory intercepts context creation at runtime level

**Pros:**
- Minimal changes to engine
- Factory intercepts all context operations
- Clear separation
- Each machine retains its own engine instance

**Cons:**
- Requires factory to wrap or proxy `ISokybotContext`

### Option B: Factory Replaces ApplicationContextAdapter

**How it works:**
1. Remove `ApplicationContextAdapter` from engine
2. Factory bundle implements context management from scratch
3. Factory uses Spring context factories directly
4. Factory publishes events and registers services

**Pros:**
- Complete separation
- No Spring dependencies in engine for context management

**Cons:**
- More invasive changes
- Factory needs to understand Spring context creation

### Option C: Hybrid - Factory Wraps Spring Context Creation

**How it works:**
1. Engine provides context adapter classes (not management)
2. Factory uses Spring's `ApplicationContext` to create contexts
3. Factory wraps context creation with event publishing
4. Factory registers as OSGi services

**Pros:**
- Leverages existing Spring infrastructure
- Clean separation
- Reuses engine's Spring configuration

**Cons:**
- Factory needs access to Spring application context

## Recommended: Option A (Factory Listens to ISokybotContext)

### Architecture Details

```
ISokybotContext Service (from engine)
    ↓
ContextFactoryManager (factory bundle)
    ├── Wraps/proxies ISokybotContext
    ├── Intercepts installGroup(), installMachine() calls
    ├── Creates contexts via Spring
    ├── Publishes events via EventAdmin
    └── Registers contexts as OSGi services

Alternative: Factory provides its own ISokybotContext implementation
    that delegates to engine's implementation but adds event publishing
```

## Implementation Plan

### Phase 1: Create Factory Bundle Structure

1. **Create `sokybot-context-factory` bundle**
   - Maven module
   - OSGi bundle configuration
   - Dependencies: `sokybot-api`, `sokybot-engine` (provided), Event Admin

2. **Create `ContextFactoryManager`**
   - OSGi DS component
   - Monitors/wraps `ISokybotContext` service
   - Coordinates context creation

3. **Create `GroupContextFactory`**
   - Creates `GroupContextAdapter` instances
   - Publishes `GROUP_CONTEXT_CREATED` event
   - Registers `IGroupContext` as OSGi service

4. **Create `MachineContextFactory`**
   - Creates `MachineContextAdapter` instances
   - Publishes `MACHINE_CONTEXT_CREATED` event
   - Registers `IMachineContext` as OSGi service

### Phase 2: Integrate with Existing Context Creation

1. **Factory wraps `ISokybotContext` operations**
   - `ISokybotContext.installGroup()` → Factory intercepts → Delegates to engine → Publishes event
   - `IGroupContext.installMachine()` → Factory intercepts → Delegates to group → Publishes event

2. **Keep existing context creation logic in engine**
   - `ApplicationContextAdapter` continues to create groups
   - `GroupContextAdapter` continues to create machines
   - Each machine creates its own engine instance (MachineContextAdapter creates Spring context)

3. **Factory adds event publishing layer**
   - Factory doesn't replace context creation
   - Factory adds event publishing on top
   - Each machine still gets its own engine instance

### Phase 3: Event Publishing

1. **Publish events on creation**
   - `GROUP_CONTEXT_CREATED` with group info
   - `MACHINE_CONTEXT_CREATED` with machine info

2. **Publish events on destruction**
   - `GROUP_CONTEXT_DESTROYED`
   - `MACHINE_CONTEXT_DESTROYED`

3. **Handle errors**
   - Publish error events if context creation fails

### Phase 4: Service Registration

1. **Register `IGroupContext` as OSGi service**
   - With service properties (groupName)
   - Unregister on destruction

2. **Register `IMachineContext` as OSGi service**
   - With service properties (groupName, machineName, fullName)
   - Unregister on destruction

## Alternative: Factory Provides ISokybotContext

Instead of wrapping, factory could:
1. Provide its own `ISokybotContext` implementation
2. Delegate actual work to engine's implementation
3. Add event publishing layer

This is cleaner but requires factory to understand all `ISokybotContext` methods.

## Questions to Resolve

1. **Should factory replace `ApplicationContextAdapter` entirely?**
   - Or wrap/decorate it?

2. **How does factory create Spring contexts?**
   - Direct access to Spring `ApplicationContext`?
   - Use engine's Spring configuration?

3. **Should contexts be registered as OSGi services?**
   - Currently they're Spring beans
   - Factory could register them as services too

4. **What about existing listeners (IGroupListener, IMachineListener)?**
   - **Migrate to EventAdmin** - this is the purpose of the factory bundle
   - Factory publishes events, plugins/bundles listen via EventHandler
   - Deprecate listener pattern over time
   - **EventAdmin is the standard OSGi pattern for plugin awareness**

## Recommendation: Component Assembly Pattern

**Create `sokybot-context-factory` bundle that:**

1. **Assembles Group Components:**
   - Persistence interface (via `IGamePersistenceFactory`) - shared by all machines
   - UI components (machine-ui bundle reacts to events)
   - Child machines

2. **Assembles Machine Components:**
   - Proxy connection (via `IProxyConnectionFactory`)
   - Engine instance (via `IEngineFactory` - **NEW**, similar to proxy factory)
   - UI components (machine-ui bundle reacts to events)
   - Other components (plugins install via events)

3. **Publishes Lifecycle Events via EventAdmin:**
   - `GROUP_CONTEXT_CREATED` / `DESTROYED`
   - `MACHINE_CONTEXT_CREATED` / `DESTROYED`
   - Replaces listener pattern (`IGroupListener`, `IMachineListener`)

4. **Enables Plugin Awareness:**
   - Third-party plugins listen to events via `EventHandler`
   - Plugins install themselves into contexts reactively
   - Standard OSGi pattern

**Key Components:**

### Group Assembly:
- Persistence: `IGamePersistenceFactory.registerGame(gamePath)`
- UI: Created by machine-ui bundle (reacts to `GROUP_CONTEXT_CREATED`)
- Machines: Assembled individually

### Machine Assembly:
- Proxy: `IProxyConnectionFactory.createConnection(machineId, listener)`
- Engine: `IEngineFactory.createEngine(machineId, proxy, groupName, machineName)` ⭐ **NEW**
- UI: Created by machine-ui bundle (reacts to `MACHINE_CONTEXT_CREATED`)

**Benefits:**
- ✅ **Clear component assembly** - explicit composition
- ✅ **Plugin awareness** - event-driven plugin installation
- ✅ **Standard OSGi pattern** - EventAdmin instead of custom listeners
- ✅ **Decoupling** - components don't depend on each other
- ✅ **Extensibility** - easy to add new components
- ✅ **Each machine has own engine** - via `IEngineFactory`
- ✅ **Third-party plugins** can react to lifecycle events

**See:** `Component_Assembly_Architecture.md` for detailed architecture
