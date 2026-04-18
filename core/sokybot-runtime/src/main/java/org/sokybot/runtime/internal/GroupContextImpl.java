package org.sokybot.runtime.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.RuntimeEntityNames;

import org.sokybot.runtime.internal.domain.GroupInfo;
import org.sokybot.runtime.internal.domain.MachineInfo;
import org.sokybot.runtime.internal.persistence.MachineInfoRepository;
import org.sokybot.runtime.internal.persistence.FileMachineInfoRepository;
import org.sokybot.runtime.ContextLifecycleEvents;
import org.sokybot.exception.NameUniquenessConstraintViolationException;
import org.sokybot.game.navigation.IRouteFinder;
import org.sokybot.game.navigation.IRouteFinderFactory;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.persistence.service.IGamePersistenceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of IGroupContext that manages a group of machines.
 * 
 * This implementation:
 * - Pure OSGi implementation - no Spring dependencies
 * - Manages child machine contexts using MachineContextFactory
 * - Publishes machine lifecycle events via EventAdmin
 * - Accesses services via OSGi service registry
 */
public class GroupContextImpl implements IGroupContext, ITranslatorRefreshable {

    private static final Logger log = LoggerFactory.getLogger(GroupContextImpl.class);

    private final GroupInfo groupInfo;
    private final BundleContext bundleContext;
    private final EventAdmin eventAdmin;
    private final IRouteFinderFactory routeFinderFactory;
    private final IGamePersistenceFactory gamePersistenceFactory;

    private MachineInfoRepository machineInfoRepo;

    private IRouteFinder routeFinder;

    // Shared translators per game (lazy initialized)
    private Map<Integer, java.util.List<org.sokybot.gameevents.events.core.IPacketTranslator>> sharedTranslators;
    private final Object translatorsLock = new Object();

    private final Lock lock = new ReentrantLock();
    /**
     * Concurrent reads safe with install/remove; install/remove still use
     * {@link #lock} for case-insensitive checks.
     */
    private final Map<String, IMachineContext> machines = new ConcurrentHashMap<>();

    private IGameDataLookup gameDataLookup;

    public GroupContextImpl(GroupInfo groupInfo,
            BundleContext bundleContext,
            EventAdmin eventAdmin,
            IRouteFinderFactory routeFinderFactory,
            IGamePersistenceFactory gamePersistenceFactory) {
        this.groupInfo = groupInfo;
        this.bundleContext = bundleContext;
        this.eventAdmin = eventAdmin;
        this.routeFinderFactory = routeFinderFactory;
        this.gamePersistenceFactory = gamePersistenceFactory;

        // Instantiate file-based repository directly (internal use only)
        this.machineInfoRepo = new FileMachineInfoRepository();

        loadMachines();
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
                    if (!RuntimeEntityNames.isValid(machineName)) {
                        log.warn("Skipping machine with invalid name '{}' in group {} (expected ^[a-zA-Z0-9_-]+$)",
                                machineName, groupInfo.getName());
                        return;
                    }
                    String existing = findExistingMachineNameIgnoreCase(machineName);
                    if (existing != null) {
                        log.warn("Skipping machine '{}' because '{}' already exists (case-insensitive collision)",
                                machineName, existing);
                        return;
                    }
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

    /**
     * Snapshot of machines at call time. The backing map is concurrent; iteration
     * is weakly consistent and may
     * reflect concurrent installs/removes. Do not assume atomicity across multiple
     * reads without external synchronization.
     */
    @Override
    public IMachineContext[] getMachines() {
        return machines.values().toArray(new IMachineContext[0]);
    }

    @Override
    public Optional<IMachineContext> findMachineCtx(String name) {
        return Optional.ofNullable(machines.get(name));
    }

    @Override
    public void installMachine(String name) {
        installMachine(name, new String[0]);
    }

    @Override
    public void installMachine(String name, String... options) {
        lock.lock();
        try {
            check(name);

            MachineInfo info = new MachineInfo(this.groupInfo.getId(), name);
            log.info("Machine info to store: {}", info);

            IMachineContext machineCtx = createMachineContext(info);

            IMachineContext existing = machines.putIfAbsent(name, machineCtx);
            if (existing != null) {
                MachineContextFactory.destroyMachineContext(machineCtx);
                throw new NameUniquenessConstraintViolationException("Machine name must be unique", name);
            }

            try {
                machineInfoRepo.save(info);
                publishMachineCreated(machineCtx);
                log.info("Machine {} installed successfully in group {}", name, groupInfo.getName());
            } catch (Exception e) {
                if (machines.remove(name, machineCtx)) {
                    MachineContextFactory.destroyMachineContext(machineCtx);
                }
                log.error("Failed to install machine: {} in group: {}", name, groupInfo.getName(), e);
                throw new RuntimeException("Failed to install machine: " + name, e);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getGamePath() {
        return groupInfo.getGamePath();
    }

    @Override
    public boolean isManualOverride() {
        return groupInfo.isManualOverride();
    }

    @Override
    public String getManualHost() {
        return groupInfo.getManualHost();
    }

    @Override
    public String getManualDivision() {
        return groupInfo.getManualDivision();
    }

    @Override
    public void removeMachine(String name) {
        lock.lock();
        try {
            IMachineContext machineCtx = machines.get(name);
            if (machineCtx != null && machines.remove(name, machineCtx)) {
                publishMachineDestroyed(machineCtx);
                MachineContextFactory.destroyMachineContext(machineCtx);
                machineInfoRepo.findByMachineName(name).ifPresent(machineInfoRepo::delete);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String name() {
        return groupInfo.getName();
    }

    @Override
    public boolean isRunning() {
        return bundleContext != null && bundleContext.getBundle().getState() == org.osgi.framework.Bundle.ACTIVE;
    }

    @Override
    public IGameDataLookup getGameDataLookup() {
        if (this.gameDataLookup == null) {
            synchronized (this) {
                if (this.gameDataLookup == null) {
                    IGamePersistenceFactory factory = this.gamePersistenceFactory;

                    if (factory == null) {
                        // Try lazy fetch if services were not ready during init
                        if (bundleContext != null) {
                            try {
                                ServiceReference<IGamePersistenceFactory> ref = bundleContext
                                        .getServiceReference(IGamePersistenceFactory.class);
                                if (ref != null) {
                                    factory = bundleContext.getService(ref);
                                }
                            } catch (Exception e) {
                                log.warn("Failed to lookup IGamePersistenceFactory", e);
                            }
                        }
                    }

                    if (factory != null) {
                        // Register game to ensure EMF is created
                        this.gameDataLookup = factory.registerGame(this.groupInfo.getGamePath());
                    } else {
                        log.warn("IGamePersistenceFactory not available - cannot load GameDataLookup");
                    }
                }
            }
        }
        return this.gameDataLookup;
    }

    @Override
    public IRouteFinder getRouteFinder() {
        if (routeFinder == null) {
            if (routeFinderFactory == null) {
                log.warn("RouteFinderFactory not available");
                return null;
            }

            IGameDataLookup lookup = getGameDataLookup();
            if (lookup != null) {
                routeFinder = routeFinderFactory.createRouteFinder(lookup);
            } else {
                log.warn("GameDataLookup not available - cannot create RouteFinder");
            }
        }
        return routeFinder;
    }

    /** Shared translator map; also exposed via {@link ITranslatorRefreshable}. */
    @Override
    public Map<Integer, java.util.List<org.sokybot.gameevents.events.core.IPacketTranslator>> getTranslators() {
        synchronized (translatorsLock) {
            if (sharedTranslators == null) {
                IGameDataLookup lookup = getGameDataLookup();
                org.sokybot.gameevents.events.core.ITranslatorFactory factory = getTranslatorFactory();
                if (factory == null) {
                    log.warn("ITranslatorFactory not available - cannot create translators");
                    sharedTranslators = Map.of();
                } else {
                    if (lookup == null) {
                        log.warn(
                                "GameDataLookup not available for group {} — creating script translators only (gateway 0xA101 etc. still work once scripts are loaded)",
                                groupInfo.getName());
                    }
                    java.util.Map<Integer, java.util.List<org.sokybot.gameevents.events.core.IPacketTranslator>> created = factory
                            .createTranslators(lookup, null);
                    if (created.isEmpty()) {
                        log.warn(
                                "Translator factory returned 0 translators for group {} — scripts may still be loading; call will retry later",
                                groupInfo.getName());
                        sharedTranslators = null;
                    } else {
                        sharedTranslators = created;
                        log.info("Created {} shared translators for game: {} (version: {})", sharedTranslators.size(),
                                lookup != null ? lookup.getGamePath() : groupInfo.getGamePath(),
                                lookup != null ? Integer.toString(lookup.getVersion()) : "n/a");
                    }
                }
            }
            return sharedTranslators != null ? sharedTranslators : java.util.Map.of();
        }
    }

    /**
     * Clears cached translators so the next {@link #getTranslators()} rebuilds
     * (e.g. after scripts finish loading).
     */
    @Override
    public void invalidateTranslators() {
        synchronized (translatorsLock) {
            sharedTranslators = null;
        }
    }

    private org.sokybot.gameevents.events.core.ITranslatorFactory getTranslatorFactory() {
        if (bundleContext != null) {
            ServiceReference<org.sokybot.gameevents.events.core.ITranslatorFactory> ref = bundleContext
                    .getServiceReference(org.sokybot.gameevents.events.core.ITranslatorFactory.class);
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
                MachineContextFactory.destroyMachineContext(machine);
            } catch (Exception e) {
                log.error("Error destroying machine context: {}", machine.name(), e);
            }
        });
        machines.clear();

        log.info("Group context destroyed: {}", groupInfo.getName());
    }

    private void check(String name) {
        Objects.requireNonNull(name, "Machine name required");

        if (name.isBlank()) {
            throw new IllegalArgumentException("Invalid machine name");
        }
        RuntimeEntityNames.validateMachineOrThrow(name);

        if (machines.containsKey(name)) {
            throw new NameUniquenessConstraintViolationException("Machine name must be unique", name);
        }
        String existingIgnoreCase = findExistingMachineNameIgnoreCase(name);
        if (existingIgnoreCase != null) {
            throw new NameUniquenessConstraintViolationException(
                    "Machine name must be unique (case-insensitive), conflicts with '" + existingIgnoreCase + "'",
                    name);
        }
    }

    private String findExistingMachineNameIgnoreCase(String candidate) {
        if (candidate == null) {
            return null;
        }
        String normalized = candidate.trim();
        for (String existing : machines.keySet()) {
            if (existing != null && existing.equalsIgnoreCase(normalized)) {
                return existing;
            }
        }
        return null;
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
