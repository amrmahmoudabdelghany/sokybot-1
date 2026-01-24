# Sokybot Engine Refactoring - Implementation Plan

**Last Updated:** Based on current codebase analysis  
**Goal:** Complete the migration from Spring-based to OSGi bundle-based architecture, making sokybot-engine headless and focused solely on workflows (Spring State Machine) and configuration.

---

## 📊 Current State Analysis

### ✅ Completed Migrations

1. **Network Infrastructure**: `sokybot-proxy` bundle exists and handles networking
2. **Game Events**: `sokybot-game-events` bundle exists for packet-to-event translation
3. **UI Bundle Structure**: `sokybot-ui` bundle exists with OSGi components
4. **Persistence Layer**: `sokybot-persistence` bundle handles data storage
5. **OSGi Integration**: Engine bundle activates and registers services via `EngineActivator`

### ❌ Remaining Issues

#### 1. UI Components Still in Engine
**Location**: `sokybot-engine/src/main/java/org/sokybot/machine/`
- **`MachinePageFactory`**: Creates Spring `@Bean` configurations for UI pages
  - `trainingPage()`, `environmentPage()`, `logPage()`, `huntingPage()`
- **Page Components** (in `machine/page/`):
  - `environmentpage/`: `EnvTab.java`, `MonsterTable.java`, `SroMapViewer.java`
  - `trainingpage/`: `AreaTab.java`, `AreaList.java`, `LocationBox.java`, `monsterpreference/`
  - `skillpage/`: `SkillTab.java`, `AttackingSettingTab.java`
  - `itempage/`: `ItemFilterTab.java`, `ItemTableModel.java`
- **Dashboard Components** (in `machine/dashboard/`):
  - `MachineDashboard.java`, `MachineControll.java`, `TrainerInfoPanel.java`
- **Interface**: `IMachinePage.java` - extends `JTabbedPane`

**Duplication Note**: Identical page classes exist in `sokybot-ui/src/main/java/org/sokybot/machine/page/` (copied but not used)

#### 2. Context Implementation Dependency on Spring Boot
**Current Implementation**:
- `MachineContextAdapter`: Creates nested Spring contexts using `SpringApplicationBuilder`
- `GroupContextAdapter`: Creates parent Spring context, then child machine contexts
- Both use `@Component` and Spring's context hierarchy

**Problems**:
- Tight coupling to Spring Boot application context lifecycle
- Nested contexts are complex and hard to manage
- Not leveraging OSGi's natural service model

**Alternative Approaches**:
- **Option A**: OSGi Service Factory Pattern (recommended)
  - Use `ServiceFactory<IMachineContext>` to create per-machine instances
  - Register with `machineName` service property
  - Group context as OSGi service, machines as factory-created services
- **Option B**: OSGi Blueprint Container
  - Use Blueprint XML for configuration
  - More declarative but adds another dependency
- **Option C**: Keep Spring but simplify
  - Move context creation out of engine
  - Create a `sokybot-context` bundle that manages Spring contexts
  - Engine only provides the `MachineConfig` class

#### 3. Network Code Still in Engine
**Location**: `sokybot-engine/src/main/java/org/sokybot/machine/network/`

**Files to Migrate/Remove**:
- `PacketDispatcher.java`: Bridges packets to game-events (currently commented/unused)
- `PacketListenerInstaller.java`: Spring BeanPostProcessor for `@PacketListener` annotation
- `PacketListenerAdapter.java`: Wraps packet listeners
- `PacketListener.java`: Annotation for packet listening
- `SimplePacketPublisher.java`: Local packet publisher (should use proxy's publisher)
- `ClientServerBridge.java`: Netty channel handler (duplicate of proxy version)
- `ClientHandler.java`: Temporarily exists, mostly commented
- `PacketEncoder.java`, `PacketDecoder.java`: Should be in proxy
- `ClientChannelInitializer.java`, `ServerChannelInitializer.java`: Should be in proxy
- `NetworkConfig.java`: Network configuration
- `NetworkAttributes.java`: Network metadata

**Note**: Engine controllers (in `machine/controller/`) use `@PacketListener` annotations that depend on `PacketListenerInstaller`. These should migrate to game events.

#### 4. Bundle Cleanup Needed
- Remove migrated network code
- Remove UI dependencies from `pom.xml`:
  - `tablelayout`
  - `swingx-all`
  - `flatlaf`
  - `flatlaf-extras`
- Remove UI-related Spring beans

#### 5. Machine Page Registration Gap
**Current State**:
- `MachinePageFactory` creates `IMachinePage` beans but they're never registered with `IMachinePageViewer`
- `EngineConfig.machinePageViewer()` retrieves OSGi service but doesn't use it
- `sokybot-ui` has `MachinePageViewer` service but it's not connected to engine pages

**Solution Needed**: Bridge Spring beans to OSGi service registration

---

## 🎯 Implementation Plan

### Phase 1: Create Dedicated Machine UI Bundle with Event Admin Integration

#### 1.1 Create `sokybot-machine-ui` Bundle
**Task**: Create a new dedicated bundle for machine-specific UI components that react to context lifecycle events via OSGi Event Admin.

**Steps**:
1. **Create new Maven module** `sokybot-machine-ui`:
   - Add to parent `pom.xml` modules list
   - Configure as OSGi bundle with `maven-bundle-plugin`
   - Dependencies:
     - `sokybot-api`
     - `sokybot-ui` (for shared UI infrastructure like `PageContainer`, `DashboardContainer`)
     - `sokybot-swing`
     - OSGi Event Admin (`org.osgi.service.event`)
     - OSGi Declarative Services annotations

2. **Define Context Lifecycle Event Topics** in `sokybot-api`:
   ```java
   public class ContextLifecycleEvents {
       // Group Context Events
       public static final String TOPIC_GROUP_CONTEXT_CREATED = "sokybot/context/group/CREATED";
       public static final String TOPIC_GROUP_CONTEXT_DESTROYED = "sokybot/context/group/DESTROYED";
       
       // Machine Context Events
       public static final String TOPIC_MACHINE_CONTEXT_CREATED = "sokybot/context/machine/CREATED";
       public static final String TOPIC_MACHINE_CONTEXT_DESTROYED = "sokybot/context/machine/DESTROYED";
       
       // Event Properties
       public static final String PROP_GROUP_NAME = "groupName";
       public static final String PROP_MACHINE_NAME = "machineName";
       public static final String PROP_FULL_NAME = "fullName"; // "groupName.machineName"
       public static final String PROP_CONTEXT = "context"; // IGroupContext or IMachineContext
   }
   ```

3. **Publish Events from Engine Context Adapters**:
   - Update `GroupContextAdapter` to publish `GROUP_CONTEXT_CREATED` when created
   - Update `GroupContextAdapter` to publish `GROUP_CONTEXT_DESTROYED` when destroyed
   - Update `MachineContextAdapter` to publish `MACHINE_CONTEXT_CREATED` when created
   - Update `MachineContextAdapter` to publish `MACHINE_CONTEXT_DESTROYED` when destroyed

#### 1.2 Create Event Handler in Machine UI Bundle
**Task**: Create OSGi EventHandler that listens to context lifecycle events and manages UI components.

**Implementation**:

1. **Create `MachineUIEventHandler`** in `sokybot-machine-ui`:
   ```java
   @Component(
       immediate = true,
       service = EventHandler.class,
       property = {
           EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED,
           EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_DESTROYED,
           EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_GROUP_CONTEXT_CREATED,
           EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_GROUP_CONTEXT_DESTROYED
       }
   )
   public class MachineUIEventHandler implements EventHandler {
       
       @Reference
       private PageContainer pageContainer;
       
       @Reference
       private DashboardContainer dashboardContainer;
       
       @Reference
       private INavTree navTree;
       
       private final Map<String, MachineUIInstance> machineUIs = new ConcurrentHashMap<>();
       
       @Override
       public void handleEvent(Event event) {
           String topic = event.getTopic();
           
           if (topic.equals(ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED)) {
               handleMachineCreated(event);
           } else if (topic.equals(ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_DESTROYED)) {
               handleMachineDestroyed(event);
           } else if (topic.equals(ContextLifecycleEvents.TOPIC_GROUP_CONTEXT_CREATED)) {
               handleGroupCreated(event);
           } else if (topic.equals(ContextLifecycleEvents.TOPIC_GROUP_CONTEXT_DESTROYED)) {
               handleGroupDestroyed(event);
           }
       }
       
       private void handleMachineCreated(Event event) {
           String groupName = (String) event.getProperty(ContextLifecycleEvents.PROP_GROUP_NAME);
           String machineName = (String) event.getProperty(ContextLifecycleEvents.PROP_MACHINE_NAME);
           IMachineContext context = (IMachineContext) event.getProperty(ContextLifecycleEvents.PROP_CONTEXT);
           
           MachineUIInstance ui = new MachineUIInstance(groupName, machineName, context);
           ui.createPages();
           machineUIs.put(context.fullName(), ui);
       }
       
       private void handleMachineDestroyed(Event event) {
           String fullName = (String) event.getProperty(ContextLifecycleEvents.PROP_FULL_NAME);
           MachineUIInstance ui = machineUIs.remove(fullName);
           if (ui != null) {
               ui.destroyPages();
           }
       }
       
       // Similar for group events...
   }
   ```

2. **Create `MachineUIInstance`**:
   - Manages all UI components for one machine
   - Creates pages via factory services
   - Registers with `PageContainer` and `DashboardContainer`

3. **Create Page Factory Services**:
   - `TrainingPageFactory`, `EnvironmentPageFactory`, `LogPageFactory`, `HuntingPageFactory`
   - Each implements `IMachinePageFactory` interface
   - Registered as OSGi services

#### 1.3 Move UI Components from Engine
**Steps**:
1. Move `machine/page/` directory to `sokybot-machine-ui/src/main/java/org/sokybot/machine/page/`
2. Move `machine/dashboard/` to `sokybot-machine-ui`
3. Move `MachinePageFactory` logic (convert to OSGi services)
4. Update `IMachinePage` interface:
   - Remove `extends JTabbedPane`
   - Make it a simple interface with `getName()`, `getIcon()`, `getComponent()`

#### 1.4 Remove UI Dependencies from Engine
**Steps**:
1. Remove UI dependencies from `sokybot-engine/pom.xml`:
   - `tablelayout`
   - `swingx-all`
   - `flatlaf`
   - `flatlaf-extras`
   - `sokybot-swing` (if only used for UI)
2. Delete UI classes from engine:
   - `MachinePageFactory.java`
   - `machine/page/` directory
   - `machine/dashboard/` directory
3. Verify no Swing imports remain in engine

---

### Phase 2: Create Context Factory Bundle for Runtime Management

#### 2.1 Architecture Decision
**Problem**: Engine should not be responsible for context lifecycle management or event publishing.

**Solution**: Create dedicated `sokybot-context-factory` bundle that:
- Manages context creation/destruction at runtime
- Publishes lifecycle events via Event Admin
- Registers contexts as OSGi services
- Keeps engine focused on machine logic only

#### 2.2 Create `sokybot-context-factory` Bundle
**Task**: Create new bundle responsible for runtime context management.

**Steps**:
1. **Create Maven module** `sokybot-context-factory`:
   - Add to parent `pom.xml` modules list
   - Configure as OSGi bundle
   - Dependencies:
     - `sokybot-api` (for interfaces)
     - `sokybot-engine` (provided - for adapter classes)
     - `sokybot-persistence` (for repositories)
     - OSGi Event Admin
     - OSGi Declarative Services
     - Spring Boot (for context creation)

2. **Create `ContextFactoryManager`**:
   - OSGi DS component
   - Monitors/wraps `ISokybotContext` service from engine
   - Coordinates context creation via factories
   - Manages context lifecycle

3. **Create `GroupContextFactory`**:
   - Creates `GroupContextAdapter` instances using Spring
   - Publishes `GROUP_CONTEXT_CREATED` event via Event Admin
   - Registers `IGroupContext` as OSGi service (optional)
   - Publishes `GROUP_CONTEXT_DESTROYED` on cleanup

4. **Create `MachineContextFactory`**:
   - Creates `MachineContextAdapter` instances using Spring
   - Publishes `MACHINE_CONTEXT_CREATED` event via Event Admin
   - Registers `IMachineContext` as OSGi service (optional)
   - Publishes `MACHINE_CONTEXT_DESTROYED` on cleanup

#### 2.3 Factory Implementation Strategy

**Approach: Factory Wraps/Proxies ISokybotContext**

The factory bundle will:
1. **Listen to `ISokybotContext` service** from engine
2. **Intercept context operations**:
   - `installGroup()` → Create group context → Publish event
   - `installMachine()` → Create machine context → Publish event
3. **Use existing Spring infrastructure** to create contexts
4. **Publish events** via Event Admin
5. **Optionally register contexts** as OSGi services

**Benefits**:
- Minimal changes to engine
- Reuses existing Spring context creation logic
- Clean separation of concerns
- Engine remains focused on machine logic

#### 2.4 Implementation Details

**`ContextFactoryManager.java`**:
```java
@Component(immediate = true)
public class ContextFactoryManager {
    
    @Reference
    private EventAdmin eventAdmin;
    
    @Reference
    private ISokybotContext sokytbotContext; // From engine
    
    @Reference
    private GroupInfoRepository groupInfoRepo;
    
    private final GroupContextFactory groupFactory;
    private final MachineContextFactory machineFactory;
    
    // Wraps ISokybotContext.installGroup() to add event publishing
    public void installGroup(String name, String gamePath, String... options) {
        // Delegate to engine's implementation
        sokytbotContext.installGroup(name, gamePath, options);
        
        // Get created context
        IGroupContext groupCtx = sokytbotContext.findGroupCtx(name).orElseThrow();
        
        // Publish event
        publishGroupCreated(groupCtx);
        
        // Register as OSGi service (optional)
        registerGroupService(groupCtx);
    }
    
    private void publishGroupCreated(IGroupContext context) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ContextLifecycleEvents.PROP_GROUP_NAME, context.name());
        properties.put(ContextLifecycleEvents.PROP_CONTEXT, context);
        properties.put(ContextLifecycleEvents.PROP_TIMESTAMP, System.currentTimeMillis());
        
        Event event = new Event(ContextLifecycleEvents.TOPIC_GROUP_CONTEXT_CREATED, properties);
        eventAdmin.postEvent(event);
    }
}
```

**Alternative Approach: Factory Provides ISokybotContext**

Instead of wrapping, factory could:
1. Provide its own `ISokybotContext` implementation
2. Delegate actual work to engine's Spring beans
3. Add event publishing layer

This requires factory to understand all `ISokybotContext` methods but provides cleaner architecture.

#### 2.5 Migration Strategy

**Step 1**: Create factory bundle structure
**Step 2**: Implement context creation (reuse engine's Spring logic)
**Step 3**: Add event publishing
**Step 4**: Optionally migrate `ApplicationContextAdapter` logic to factory
**Step 5**: Remove context lifecycle management from engine

#### 2.6 Benefits of Factory Bundle Approach

1. **Separation of Concerns**: Engine = machine logic, Factory = runtime management
2. **Single Responsibility**: Each bundle has one clear purpose
3. **Loose Coupling**: Engine doesn't know about Event Admin
4. **Testability**: Each component can be tested independently
5. **Extensibility**: Other bundles can listen to lifecycle events
6. **Maintainability**: Changes to runtime management don't affect engine

#### 2.3 Recommended Approach: Hybrid OSGi-Spring Model

**Architecture**:
```
OSGi Layer (Service Registry)
  ├── IGroupContext service (per group)
  └── IMachineContext service (per machine, via ServiceFactory)

Spring Layer (Per Machine)
  └── Machine Spring Context
      ├── State Machine Configuration
      ├── Actuators
      ├── Controllers
      └── Services
```

**Implementation**:

1. **Keep Spring for Machine Logic**:
   - Spring State Machine is valuable and works well
   - Keep `MachineConfig` and machine-level Spring context
   - **But**: Don't nest contexts

2. **Use OSGi for Context Management**:
   - `GroupContextAdapter` becomes OSGi service
   - `MachineContextAdapter` creates Spring context internally but doesn't nest
   - Machine contexts are independent Spring Boot apps that share group beans via OSGi services

3. **Service Registration Pattern**:
   ```java
   @Component
   public class MachineContextAdapter implements IMachineContext {
       private ConfigurableApplicationContext springCtx;
       
       @PostConstruct
       public void start() {
           // Create independent Spring context (not nested)
           springCtx = new SpringApplicationBuilder(MachineConfig.class)
               .properties(...)
               .run();
               
           // Register OSGi services from Spring beans
           registerServices();
       }
   }
   ```

4. **Group Context Simplification**:
   - `GroupContextAdapter` manages machines via OSGi service registry lookup
   - Each machine is independent, no parent-child context relationship
   - Shared services (like `ISroDAO`) accessed via OSGi service registry

**Benefits**:
- Reduces Spring context complexity
- Better OSGi integration
- Machines are truly independent
- Easier to test and debug

#### 2.3 Alternative: Pure OSGi (if preferred)
If Spring Boot is considered too heavy:
1. Replace Spring State Machine with a custom state machine or lightweight library
2. Use OSGi Declarative Services for dependency injection
3. Use OSGi Configuration Admin for settings
4. More work but cleaner separation

**Recommendation**: Keep Spring for machine logic (state machine, actuators), use OSGi for lifecycle and service discovery.

---

### Phase 3: Complete Network Migration

#### 3.1 Migrate Packet Listener Infrastructure
**Current**: `PacketListenerInstaller` uses Spring BeanPostProcessor to find `@PacketListener` methods

**New Approach**: Game Events Pattern

**Steps**:
1. **Remove from Engine**:
   - Delete `PacketListenerInstaller.java`
   - Delete `PacketListenerAdapter.java`
   - Delete `PacketListener.java` annotation (or move to deprecated package)

2. **Migrate Controllers to Game Events**:
   - Find all classes with `@PacketListener` methods
   - Convert packet listeners to `@EventListener` methods on game events
   - Example:
     ```java
     // OLD:
     @PacketListener(opcode = 0x1234)
     public void handlePacket(ImmutablePacket packet) { ... }
     
     // NEW:
     @EventListener
     public void handleEvent(EntitySpawnEvent event) { ... }
     ```

3. **Ensure PacketDispatcher Works**:
   - Uncomment/complete `PacketDispatcher` in engine
   - It should forward packets from `IPacketPublisher` to `GameEventPublisher` (OSGi service)
   - Remove if proxy already publishes events directly

4. **Verify Game Events Bundle**:
   - Ensure `sokybot-game-events` properly translates all needed packets
   - Add missing translators if needed

#### 3.2 Remove Network Infrastructure Code
**Files to Delete**:
- `machine/network/ClientServerBridge.java` (duplicate)
- `machine/network/ClientHandler.java` (temporary)
- `machine/network/PacketEncoder.java`, `PacketDecoder.java` (in proxy)
- `machine/network/ClientChannelInitializer.java`, `ServerChannelInitializer.java` (in proxy)
- `machine/network/NetworkConfig.java` (if not needed)
- `machine/network/SimplePacketPublisher.java` (use proxy's)

**Keep Temporarily** (if needed for transition):
- `PacketDispatcher.java` (until game events fully integrated)

#### 3.3 Update Engine Configuration
- Remove network-related beans from `EngineConfig`
- Ensure `IPacketPublisher` comes from `IProxyConnection` only
- Remove any network channel setup

---

### Phase 4: Settings/Persistence Plugin Support

#### 4.1 Current Problem
- `Settings` entity is hardcoded in `sokybot-persistence`
- Plugin developers can't persist their own settings

#### 4.2 Solution: Generic Settings Service

**Create `ISettingsService` in `sokybot-api`**:
```java
public interface ISettingsService {
    <T> T getSettings(String key, Class<T> type);
    <T> void saveSettings(String key, T settings);
    void deleteSettings(String key);
}
```

**Implementation in `sokybot-persistence`**:
- Store settings as JSON in database
- Key format: `{pluginId}.{settingsId}` or `machine.{machineId}.{settingsId}`
- Use JPA entity with key-value storage

**Migration**:
- Convert `Settings` entity to use `ISettingsService`
- Create adapter for backward compatibility
- Deprecate direct entity access

---

### Phase 5: Machine Page Instance Strategy

#### 5.1 Decision Required
**Question**: Per-machine UI pages or shared UI with switcher?

#### 5.2 Analysis

**Option A: Per-Machine Pages** (Recommended)
- **Pros**:
  - Complete isolation
  - Can have different UI per machine type
  - Easier to manage state
  - Better for multi-machine scenarios
- **Cons**:
  - More memory usage
  - Slightly more complex registration
- **Implementation**:
  - Each machine creates its own page instances
  - Pages registered with machine-specific path: `{group}.{machine}.{page}`
  - `IMachinePageViewer` factory creates viewer per machine

**Option B: Shared UI with Switcher**
- **Pros**:
  - Less memory
  - Single UI to maintain
- **Cons**:
  - State management complexity
  - Must handle switching between machines
  - Race conditions if machines update simultaneously
- **Implementation**:
  - Single set of page instances
  - Controller switches data source based on selected machine
  - Pages update when machine selection changes

#### 5.3 Recommendation: Option A (Per-Machine)
- Better aligns with OSGi service-per-instance model
- Cleaner architecture
- Already partially implemented (machine-specific paths)

**Implementation**:
1. `IMachinePageViewer` becomes a service factory
2. Each machine gets its own viewer instance
3. Pages are created per machine
4. UI components are bound to machine context

---

### Phase 6: Cleanup and Verification

#### 6.1 Remove Migrated Code
- Delete network package from engine
- Delete UI components from engine
- Remove commented-out code
- Remove unused imports

#### 6.2 Update Dependencies
- Remove UI dependencies from `pom.xml`
- Update import statements
- Verify no circular dependencies

#### 6.3 Testing
- Test machine creation/destruction
- Test UI page registration
- Test game event flow
- Test settings persistence
- Test multi-machine scenarios

#### 6.4 Documentation
- Update architecture documentation
- Document new service interfaces
- Migration guide for plugin developers

---

## 📋 Implementation Priority

### High Priority (Blocking Headless Goal)
1. **Phase 2**: Create Context Factory Bundle (enables event-driven architecture)
2. **Phase 1**: Extract UI Components (must remove UI from engine)
3. **Phase 3**: Complete Network Migration (must remove network code)
4. **Phase 5**: Implement Machine Page Strategy (needed for UI to work)

### Medium Priority (Architecture Quality)
5. **Phase 2 (continued)**: Refactor Context Implementation (improves maintainability)
6. **Phase 4**: Settings Plugin Support (enables extensibility)

### Low Priority (Polish)
7. **Phase 6**: Cleanup and Verification (ongoing)

---

## 🔄 Migration Strategy

### Step-by-Step Approach

1. **Start with Phase 2 (Context Factory)** ⭐ **NEW**:
   - Creates foundation for event-driven architecture
   - Enables all other phases
   - Factory bundle manages runtime lifecycle
   - **Critical**: This must be done first to enable event-driven UI

2. **Then Phase 1 (UI)**:
   - Machine UI bundle listens to factory events
   - Reactive UI creation based on context lifecycle
   - Requires Phase 2 events to be working

3. **Then Phase 5 (Page Strategy)**:
   - Finalize UI architecture
   - Implement per-machine page instances
   - Pages created via factory services

4. **Then Phase 3 (Network)**:
   - Remove network code from engine
   - Complete game events migration
   - Can be done incrementally

5. **Then Phase 4 (Settings)**:
   - Add extensibility for plugin developers
   - Backward compatibility layer

6. **Finally Phase 6 (Cleanup)**:
   - Remove all migrated code from engine
   - Final verification
   - Engine is truly headless

---

## ❓ Open Questions

1. **Parent-Child Context Relationship**: 
   - Is it needed? Can machines share services via OSGi instead?
   - **Recommendation**: Remove parent-child, use OSGi services

2. **Spring Boot in OSGi**:
   - Keep nested contexts or independent contexts?
   - **Recommendation**: Independent contexts, OSGi for coordination

3. **Page Instance Strategy**:
   - Per-machine or shared?
   - **Recommendation**: Per-machine (Option A)

4. **Packet Listener Migration**:
   - Timeline for migrating all controllers?
   - **Recommendation**: Migrate incrementally, keep compatibility layer temporarily

---

## 📝 Notes

- The engine should only contain:
  - State machine configuration (`MachineConfig`)
  - Actuators (workflow actions)
  - Controllers (event handlers)
  - Services (business logic)
  - Models (domain objects)

- The engine should NOT contain:
  - UI components
  - Network infrastructure
  - Context management (should be in separate bundle or OSGi layer)

- Current dependencies show good separation is possible:
  - `sokybot-proxy` handles networking
  - `sokybot-game-events` handles packet translation
  - `sokybot-ui` handles UI
  - Engine just needs to consume these services

---

## 🎯 Success Criteria

1. ✅ No UI classes in `sokybot-engine`
2. ✅ No network code in `sokybot-engine` (except game event consumption)
3. ✅ Context creation is simple and OSGi-native
4. ✅ Machines can be created/destroyed independently
5. ✅ UI pages register correctly per machine
6. ✅ All packet listeners use game events
7. ✅ Plugin developers can persist settings
8. ✅ Engine is truly headless (no Swing dependencies)
