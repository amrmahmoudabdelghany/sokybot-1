package org.sokybot.runtime.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.ui.api.IPageViewer;
import org.sokybot.runtime.internal.domain.GroupInfo;
import org.sokybot.runtime.internal.domain.MachineInfo;
import org.sokybot.runtime.internal.persistence.MachineInfoRepository;
import org.sokybot.runtime.internal.persistence.FileMachineInfoRepository;
import org.sokybot.runtime.ContextLifecycleEvents;
import org.sokybot.runtime.internal.MachineContextFactory;
// import org.sokybot.engine.SpringGroupContextWrapper;
import org.sokybot.exception.NameUniquenessConstraintViolationException;
import org.sokybot.game.navigation.IRuteFinder;
import org.sokybot.game.navigation.IRuteFinderFactory;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.persistence.service.IGamePersistenceFactory;
import org.sokybot.service.ISroDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of IGroupContext that manages a group of machines.
 * 
 * This implementation:
 * - Uses Spring context created by IGroupContextFactory (from engine bundle)
 * - Manages child machine contexts using IMachineContextFactory
 * - Publishes machine lifecycle events via EventAdmin
 * - Accesses services via OSGi or Spring context wrapper
 */
public class GroupContextImpl implements IGroupContext {
    
    private static final Logger log = LoggerFactory.getLogger(GroupContextImpl.class);
    
    private final GroupInfo groupInfo;
    private final SpringGroupContextWrapper springWrapper;
    private final BundleContext bundleContext;
    
    private MachineInfoRepository machineInfoRepo;
    private EventAdmin eventAdmin;
    private IRuteFinderFactory ruteFinderFactory;
    private IGamePersistenceFactory gamePersistenceFactory;
    
    private IRuteFinder ruteFinder;
    
    // Shared translators per game (lazy initialized)
    private Map<Integer, org.sokybot.gameevents.events.core.IPacketTranslator> sharedTranslators;
    private final Object translatorsLock = new Object();
    
    private final Lock lock = new ReentrantLock();
    private final Map<String, IMachineContext> machines = new HashMap<>();
    
    public GroupContextImpl(GroupInfo groupInfo, SpringGroupContextWrapper springWrapper, BundleContext bundleContext) {
        this.groupInfo = groupInfo;
        this.springWrapper = springWrapper;
        this.bundleContext = bundleContext;
        
        // Instantiate file-based repository directly (internal use only)
        this.machineInfoRepo = new FileMachineInfoRepository();
        
        initializeServices();
        loadMachines();
    }
    
    private void initializeServices() {
        // Get EventAdmin from OSGi service registry
        if (bundleContext != null) {
            try {
                // Get EventAdmin from OSGi service registry
                ServiceReference<EventAdmin> eventAdminRef = bundleContext.getServiceReference(EventAdmin.class);
                if (eventAdminRef != null) {
                    eventAdmin = bundleContext.getService(eventAdminRef);
                }
                
                // Get IRuteFinderFactory from OSGi service registry
                ServiceReference<IRuteFinderFactory> ruteFinderFactoryRef = bundleContext.getServiceReference(IRuteFinderFactory.class);
                if (ruteFinderFactoryRef != null) {
                    ruteFinderFactory = bundleContext.getService(ruteFinderFactoryRef);
                }
                
                // Get IGamePersistenceFactory from OSGi service registry
                ServiceReference<IGamePersistenceFactory> persistenceFactoryRef = bundleContext.getServiceReference(IGamePersistenceFactory.class);
                if (persistenceFactoryRef != null) {
                    gamePersistenceFactory = bundleContext.getService(persistenceFactoryRef);
                }
            } catch (Exception e) {
                log.warn("Services not available from OSGi", e);
            }
        }
    }
    
    private void loadMachines() {
        if (machineInfoRepo == null) {
            log.warn("MachineInfoRepository not available - cannot load machines");
            return;
        }
        
        log.info("GroupInfo: {}", this.groupInfo);
        try {
            machineInfoRepo.findByGroupId(this.groupInfo.getId()).forEach((machine) -> {
                log.info("Detected Machine [x] {}", machine);
                
                String machineName = machine.getMachineName();
                try {
                    check(machineName);
                    
                    // Create machine context
                    IMachineContext machineCtx = createMachineContext(machine);
                    machines.put(machineName, machineCtx);
                    publishMachineCreated(machineCtx);
                } catch (Exception e) {
                    log.error("Failed to load machine: {}", machineName, e);
                }
            });
        } catch (Exception e) {
            log.error("Error loading machines for group: {}", groupInfo.getName(), e);
        }
        
        log.info("Machines have been loaded for group: {}", groupInfo.getName());
    }
    
    private IMachineContext createMachineContext(MachineInfo machineInfo) {
        return MachineContextFactory.createMachineContext(machineInfo, this, bundleContext);
    }
    
    @Override
    public ISroDAO getGameDAO() {
        // Get from Spring context (via wrapper)
        return springWrapper.getGameDAO();
    }
    
    @Override
    public IPageViewer pageViewer() {
        // Get from OSGi service or Spring context (via wrapper)
        return springWrapper.getPageViewer();
    }
    
    @Override
    public IMachineContext[] getMachines() {
        synchronized (machines) {
            return machines.values().toArray(new IMachineContext[0]);
        }
    }
    
    @Override
    public Optional<IMachineContext> findMachineCtx(String name) {
        synchronized (machines) {
            return Optional.ofNullable(machines.get(name));
        }
    }
    
    @Override
    public void installMachine(String name) {
        installMachine(name, new String[0]);
    }
    
    @Override
    public void installMachine(String name, String... options) {
        try {
            lock.lock();
            check(name);
            
            MachineInfo info = new MachineInfo(this.groupInfo.getId(), name);
            log.info("Machine info to store: {}", info);
            
            IMachineContext machineCtx = createMachineContext(info);
            
            machines.put(name, machineCtx);
            
            // Save to database
            machineInfoRepo.save(info);
            
            publishMachineCreated(machineCtx);
            log.info("Machine {} installed successfully in group {}", name, groupInfo.getName());
        } catch (Exception e) {
            log.error("Failed to install machine: {} in group: {}", name, groupInfo.getName(), e);
            throw new RuntimeException("Failed to install machine: " + name, e);
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void addMachineListener(org.sokybot.IMachineListener machineListener) {
        // Deprecated: Use EventAdmin to listen to MACHINE_CONTEXT_CREATED/DESTROYED events instead
        log.warn("addMachineListener() is deprecated. Use EventAdmin to listen to context lifecycle events.");
    }
    
    @Override
    public void removeMachineListener(org.sokybot.IMachineListener machineListener) {
        // Deprecated: Use EventAdmin to listen to MACHINE_CONTEXT_CREATED/DESTROYED events instead
        log.warn("removeMachineListener() is deprecated. Use EventAdmin to listen to context lifecycle events.");
    }
    
    @Override
    public String name() {
        return groupInfo.getName();
    }
    
    @Override
    public boolean isRunning() {
        return springWrapper != null && springWrapper.isRunning();
    }
    
    private IGameDataLookup gameDataLookup;

    @Override
    public IGameDataLookup getGameDataLookup() {
         if (this.gameDataLookup == null) {
              if (this.gamePersistenceFactory == null) {
                  // Try lazy fetch if services were not ready during init
                  if (bundleContext != null) {
                      ServiceReference<IGamePersistenceFactory> ref = bundleContext.getServiceReference(IGamePersistenceFactory.class);
                      if (ref != null) {
                          gamePersistenceFactory = bundleContext.getService(ref);
                      }
                  }
              }
              
              if (this.gamePersistenceFactory != null) {
                  this.gameDataLookup = this.gamePersistenceFactory.getLookup(this.groupInfo.getGamePath());
              }
         }
         return this.gameDataLookup;
    }

    @Override
    public IRuteFinder getRuteFinder() {
        if (ruteFinder == null) {
            if (ruteFinderFactory == null) {
                 log.warn("RuteFinderFactory not available");
                 return null;
            }
            
            IGameDataLookup lookup = getGameDataLookup();
            if (lookup != null) {
                ruteFinder = ruteFinderFactory.createRuteFinder(lookup);
            } else {
                log.warn("GameDataLookup not available - cannot create RuteFinder");
            }
        }
        return ruteFinder;
    }
    
    @Override
    public Map<Integer, org.sokybot.gameevents.events.core.IPacketTranslator> getTranslators() {
        if (sharedTranslators == null) {
            synchronized (translatorsLock) {
                if (sharedTranslators == null) {
                    // Lazy initialization - create once per game
                    IGameDataLookup lookup = getGameDataLookup();
                    if (lookup != null) {
                        org.sokybot.gameevents.events.core.ITranslatorFactory factory = getTranslatorFactory();
                        if (factory != null) {
                            sharedTranslators = factory.createTranslators(lookup, null);
                            log.info("Created {} shared translators for game: {} (version: {})", 
                                    sharedTranslators.size(), groupInfo.getGamePath(), lookup.getVersion());
                        } else {
                            log.warn("ITranslatorFactory not available - cannot create translators");
                            sharedTranslators = Map.of();
                        }
                    } else {
                        log.warn("GameDataLookup not available - cannot create translators");
                        sharedTranslators = Map.of();
                    }
                }
            }
        }
        return sharedTranslators;
    }
    
    private org.sokybot.gameevents.events.core.ITranslatorFactory getTranslatorFactory() {
        if (bundleContext != null) {
            ServiceReference<org.sokybot.gameevents.events.core.ITranslatorFactory> ref = 
                bundleContext.getServiceReference(org.sokybot.gameevents.events.core.ITranslatorFactory.class);
            if (ref != null) {
                return bundleContext.getService(ref);
            }
        }
        return null;
    }
    
    public void destroy() {
        log.info("Destroying group context: {}", groupInfo.getName());
        
        // Publish events and close all machine contexts
        machines.values().forEach(machine -> {
            try {
                publishMachineDestroyed(machine);
                if (machineContextFactory != null) {
                    machineContextFactory.destroyMachineContext(machine);
                }
            } catch (Exception e) {
                log.error("Error destroying machine context: {}", machine.name(), e);
            }
        });
        machines.clear();
        
        // Close Spring context (via wrapper)
        if (springWrapper != null) {
            springWrapper.destroy();
        }
        
        log.info("Group context destroyed: {}", groupInfo.getName());
    }
    
    private void check(String name) {
        Objects.requireNonNull(name, "Machine name required");
        
        if (name.isBlank()) {
            throw new IllegalArgumentException("Invalid machine name");
        }
        
        if (machines.containsKey(name)) {
            throw new NameUniquenessConstraintViolationException("Machine name must be unique", name);
        }
    }
    
    private void publishMachineCreated(IMachineContext context) {
        if (eventAdmin == null) {
            return;
        }
        
        Map<String, Object> properties = new HashMap<>();
        String fullName = context.fullName();
        String[] parts = fullName.split("\\.");
        String groupName = parts.length > 0 ? parts[0] : "";
        String machineName = parts.length > 1 ? parts[1] : context.name();
        
        properties.put(ContextLifecycleEvents.PROP_GROUP_NAME, groupName);
        properties.put(ContextLifecycleEvents.PROP_MACHINE_NAME, machineName);
        properties.put(ContextLifecycleEvents.PROP_FULL_NAME, fullName);
        properties.put(ContextLifecycleEvents.PROP_CONTEXT, context);
        properties.put(ContextLifecycleEvents.PROP_TIMESTAMP, System.currentTimeMillis());
        
        Event event = new Event(ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED, properties);
        eventAdmin.postEvent(event);
        
        log.debug("Published MACHINE_CONTEXT_CREATED event for: {}", fullName);
    }
    
    private void publishMachineDestroyed(IMachineContext context) {
        if (eventAdmin == null) {
            return;
        }
        
        Map<String, Object> properties = new HashMap<>();
        String fullName = context.fullName();
        String[] parts = fullName.split("\\.");
        String groupName = parts.length > 0 ? parts[0] : "";
        String machineName = parts.length > 1 ? parts[1] : context.name();
        
        properties.put(ContextLifecycleEvents.PROP_GROUP_NAME, groupName);
        properties.put(ContextLifecycleEvents.PROP_MACHINE_NAME, machineName);
        properties.put(ContextLifecycleEvents.PROP_FULL_NAME, fullName);
        properties.put(ContextLifecycleEvents.PROP_TIMESTAMP, System.currentTimeMillis());
        
        Event event = new Event(ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_DESTROYED, properties);
        eventAdmin.postEvent(event);
        
        log.debug("Published MACHINE_CONTEXT_DESTROYED event for: {}", fullName);
    }
}
