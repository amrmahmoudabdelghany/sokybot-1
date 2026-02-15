package org.sokybot.runtime.internal;

import org.sokybot.commons.SilkroadUtils;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import java.util.HashMap;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.runtime.internal.domain.GroupInfo;
import org.sokybot.runtime.internal.persistence.GroupInfoRepository;
import org.sokybot.runtime.internal.persistence.FileGroupInfoRepository;
import org.sokybot.runtime.ContextLifecycleEvents;
import org.sokybot.exception.InvalidGameReferenceException;
import org.sokybot.exception.NameUniquenessConstraintViolationException;

import org.sokybot.runtime.IGroupContextFactory;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ISokybotContext that manages the entire runtime.
 */
@Component(immediate = true, service = ISokybotContext.class)
public class SokybotContextImpl implements ISokybotContext {

    private static final Logger log = LoggerFactory.getLogger(SokybotContextImpl.class);

    private GroupInfoRepository groupInfoRepo;
    private IGroupContextFactory groupContextFactory;
    private EventAdmin eventAdmin;
    private BundleContext bundleContext;

    private final Lock lock = new ReentrantLock();
    private final Map<String, IGroupContext> groups = new ConcurrentHashMap<>();

    @Reference
    public void setGroupContextFactory(IGroupContextFactory factory) {
        this.groupContextFactory = factory;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.groupInfoRepo = new FileGroupInfoRepository();
        loadExistingGroups();
        log.info("SokybotContextImpl activated with {} groups", groups.size());
    }

    @Deactivate
    public void deactivate() {
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

    private void loadExistingGroups() {
        if (groupInfoRepo == null)
            return;
        log.info("Loading existing groups from database...");
        try {
            groupInfoRepo.findAll().forEach((groupInfo) -> {
                try {
                    check(groupInfo.getName(), groupInfo.getGamePath());
                    IGroupContext groupCtx = groupContextFactory.createGroupContext(groupInfo, bundleContext);
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
    public String name() {
        return "SokybotContext";
    }

    @Override
    public boolean isRunning() {
        return true;
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
        try {
            installGroupInternal(groupName, gamePath);
        } catch (Exception e) {
            log.error("Failed to install group: {}", groupName, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void installGroup(String groupName, String gamePath, String... options) {
        installGroup(groupName, gamePath);
    }

    private IGroupContext installGroupInternal(String groupName, String gamePath) throws InvalidGameReferenceException {
        lock.lock();
        try {
            if (groups.containsKey(groupName)) {
                throw new NameUniquenessConstraintViolationException("Group name already exists: " + groupName,
                        groupName);
            }
            check(groupName, gamePath);
            GroupInfo groupInfo = new GroupInfo();
            groupInfo.setName(groupName);
            groupInfo.setGamePath(gamePath);
            IGroupContext groupCtx = groupContextFactory.createGroupContext(groupInfo, bundleContext);
            groups.put(groupName, groupCtx);
            groupInfoRepo.save(groupInfo);
            publishGroupCreated(groupCtx);
            return groupCtx;
        } finally {
            lock.unlock();
        }
    }

    public void removeGroup(String groupName) {
        lock.lock();
        try {
            IGroupContext groupCtx = groups.remove(groupName);
            if (groupCtx != null) {
                publishGroupDestroyed(groupCtx);
                groupContextFactory.destroyGroupContext(groupCtx);
                groupInfoRepo.findByName(groupName).ifPresent(groupInfoRepo::delete);
            }
        } finally {
            lock.unlock();
        }
    }

    private void check(String groupName, String gamePath) throws InvalidGameReferenceException {
        if (groupName == null || groupName.trim().isEmpty())
            throw new IllegalArgumentException("Group name cannot be null or empty");
        if (gamePath == null || gamePath.trim().isEmpty())
            throw new IllegalArgumentException("Game path cannot be null or empty");
        if (!SilkroadUtils.isValidSilkroadDirectory(gamePath))
            throw new InvalidGameReferenceException("Invalid game path: " + gamePath, gamePath);
    }

    private void publishGroupCreated(IGroupContext group) {
        Map<String, Object> props = new HashMap<>();
        props.put(ContextLifecycleEvents.PROP_GROUP_NAME, group.name());
        props.put(ContextLifecycleEvents.PROP_CONTEXT, group);
        props.put(ContextLifecycleEvents.PROP_TIMESTAMP, System.currentTimeMillis());
        Event event = new Event(ContextLifecycleEvents.TOPIC_GROUP_CONTEXT_CREATED, props);
        if (eventAdmin != null)
            eventAdmin.postEvent(event);
    }

    private void publishGroupDestroyed(IGroupContext group) {
        Map<String, Object> props = new HashMap<>();
        props.put(ContextLifecycleEvents.PROP_GROUP_NAME, group.name());
        props.put(ContextLifecycleEvents.PROP_CONTEXT, group);
        props.put(ContextLifecycleEvents.PROP_TIMESTAMP, System.currentTimeMillis());
        Event event = new Event(ContextLifecycleEvents.TOPIC_GROUP_CONTEXT_DESTROYED, props);
        if (eventAdmin != null)
            eventAdmin.postEvent(event);
    }
}
