package org.sokybot.runtime.internal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import java.util.HashMap;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.app.domain.GroupInfo;
import org.sokybot.runtime.ContextLifecycleEvents;
import org.sokybot.exception.InvalidGameReferenceException;
import org.sokybot.exception.NameUniquenessConstraintViolationException;
import org.sokybot.persistence.service.GroupInfoRepository;
import org.sokybot.service.IMainFrameConfigurator;
import org.sokybot.context.IGroupContextFactory;
import org.sokybot.utils.SilkroadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ISokybotContext that manages the entire runtime.
 * 
 * This implementation:
 * - Manages all group contexts
 * - Creates group contexts via IGroupContextFactory
 * - Registers itself as OSGi service
 * - Publishes group lifecycle events via EventAdmin
 */
@Component(immediate = true, service = ISokybotContext.class)
public class SokybotContextImpl implements ISokybotContext {
    
    private static final Logger log = LoggerFactory.getLogger(SokybotContextImpl.class);
    
    @Reference
    private GroupInfoRepository groupInfoRepo;
    
    @Reference
    private IGroupContextFactory groupContextFactory;
    
    @Reference(required = false)
    private IMainFrameConfigurator frameConfigurator;
    
    @Reference(required = false)
    private EventAdmin eventAdmin;
    
    private BundleContext bundleContext;
    private ServiceRegistration<ISokybotContext> serviceRegistration;
    
    private final Lock lock = new ReentrantLock();
    private final Map<String, IGroupContext> groups = new ConcurrentHashMap<>();
    
    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        
        // IGroupContextFactory is injected via @Reference
        // It's provided by engine bundle and creates Spring contexts
        
        // Register as OSGi service
        registerOSGiService();
        
        // Load existing groups from database
        loadExistingGroups();
        
        log.info("SokybotContextImpl activated with {} groups", groups.size());
    }
    
    @Deactivate
    public void deactivate() {
        // Unregister OSGi service
        if (serviceRegistration != null) {
            try {
                serviceRegistration.unregister();
                log.info("ISokybotContext OSGi service unregistered");
            } catch (Exception e) {
                log.error("Error unregistering OSGi service", e);
            }
        }
        
        // Publish events and close all group contexts
        groups.values().forEach(group -> {
            try {
                publishGroupDestroyed(group);
                groupContextFactory.destroyGroupContext(group);
            } catch (Exception e) {
                log.error("Error destroying group context: {}", group.name(), e);
            }
        });
        groups.clear();
        
        log.info("SokybotContextImpl deactivated");
    }
    
    private void registerOSGiService() {
        if (bundleContext != null) {
            try {
                serviceRegistration = bundleContext.registerService(
                        ISokybotContext.class,
                        this,
                        null);
                log.info("ISokybotContext registered as OSGi service");
            } catch (Exception e) {
                log.error("Failed to register ISokybotContext as OSGi service", e);
            }
        }
    }
    
    private void loadExistingGroups() {
        if (groupInfoRepo == null) {
            log.warn("GroupInfoRepository not available - cannot load groups");
            return;
        }
        
        log.info("Loading existing groups from database...");
        try {
            groupInfoRepo.findAll().forEach((groupInfo) -> {
                try {
                    check(groupInfo.getName(), groupInfo.getGamePath());
                    IGroupContext groupCtx = groupContextFactory.createGroupContext(
                            groupInfo, bundleContext);
                    this.groups.put(groupInfo.getName(), groupCtx);
                    publishGroupCreated(groupCtx);
                    log.info("Group {} has been loaded", groupInfo.getName());
                } catch (Exception e) {
                    log.error("Failed to load group: {}", groupInfo.getName(), e);
                }
            });
        } catch (Exception e) {
            log.error("Error loading groups from database", e);
        }
    }
    
    @Override
    public IMainFrameConfigurator getFrameConfigurator() {
        return frameConfigurator;
    }
    
    @Override
    public IGroupContext[] getGroups() {
        return groups.values().toArray(new IGroupContext[0]);
    }
    
    @Override
    public String[] listNames() {
        return groups.keySet().toArray(new String[0]);
    }
    
    @Override
    public Optional<IGroupContext> findGroupCtx(String name) {
        return Optional.ofNullable(groups.get(name));
    }
    
    @Override
    public void installGroup(String groupName, String gamePath) {
        installGroup(groupName, gamePath, new String[0]);
    }
    
    @Override
    public void installGroup(String groupName, String gamePath, String... options) {
        log.info("Installing new machine group with name {} at {}", groupName, gamePath);
        try {
            lock.lock();
            check(groupName, gamePath);
            
            GroupInfo info = new GroupInfo(groupName, gamePath);
            IGroupContext groupContext = groupContextFactory.createGroupContext(info, bundleContext);
            
            // Save to database
            groupInfoRepo.save(info);
            groups.put(groupName, groupContext);
            
            publishGroupCreated(groupContext);
            log.info("Group {} installed successfully", groupName);
        } catch (Exception e) {
            log.error("Failed to install group: {}", groupName, e);
            throw new RuntimeException("Failed to install group: " + groupName, e);
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void addGroupListener(org.sokybot.IGroupListener listener) {
        // Deprecated: Use EventAdmin to listen to GROUP_CONTEXT_CREATED/DESTROYED events instead
        log.warn("addGroupListener() is deprecated. Use EventAdmin to listen to context lifecycle events.");
    }
    
    @Override
    public void removeGroupListener(org.sokybot.IGroupListener listener) {
        // Deprecated: Use EventAdmin to listen to GROUP_CONTEXT_CREATED/DESTROYED events instead
        log.warn("removeGroupListener() is deprecated. Use EventAdmin to listen to context lifecycle events.");
    }
    
    @Override
    public boolean isRunning() {
        return bundleContext != null && bundleContext.getBundle().getState() == org.osgi.framework.Bundle.ACTIVE;
    }
    
    @Override
    public String name() {
        return "SokyBot";
    }
    
    private void check(String name, String gamePath) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group name cannot be blank");
        }
        
        if (gamePath == null || gamePath.isBlank()) {
            throw new IllegalArgumentException("Game path cannot be blank");
        }
        
        if (groups.containsKey(name)) {
            throw new NameUniquenessConstraintViolationException(
                    "Each machine group is identified by its name, so the name must be unique", name);
        }
        
        if (!SilkroadUtils.isValidSilkroadDirectory(gamePath)) {
            throw new InvalidGameReferenceException("Invalid game directory " + gamePath, gamePath);
        }
    }
    
    private void publishGroupCreated(IGroupContext context) {
        if (eventAdmin == null) {
            return;
        }
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(ContextLifecycleEvents.PROP_GROUP_NAME, context.name());
        properties.put(ContextLifecycleEvents.PROP_CONTEXT, context);
        properties.put(ContextLifecycleEvents.PROP_TIMESTAMP, System.currentTimeMillis());
        
        Event event = new Event(ContextLifecycleEvents.TOPIC_GROUP_CONTEXT_CREATED, properties);
        eventAdmin.postEvent(event);
        
        log.debug("Published GROUP_CONTEXT_CREATED event for: {}", context.name());
    }
    
    private void publishGroupDestroyed(IGroupContext context) {
        if (eventAdmin == null) {
            return;
        }
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(ContextLifecycleEvents.PROP_GROUP_NAME, context.name());
        properties.put(ContextLifecycleEvents.PROP_TIMESTAMP, System.currentTimeMillis());
        
        Event event = new Event(ContextLifecycleEvents.TOPIC_GROUP_CONTEXT_DESTROYED, properties);
        eventAdmin.postEvent(event);
        
        log.debug("Published GROUP_CONTEXT_DESTROYED event for: {}", context.name());
    }
}
