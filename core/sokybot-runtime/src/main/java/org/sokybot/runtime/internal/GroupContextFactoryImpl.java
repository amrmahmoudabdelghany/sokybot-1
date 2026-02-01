package org.sokybot.runtime.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.internal.domain.GroupInfo;
import org.sokybot.runtime.IGroupContextFactory;
import org.sokybot.game.navigation.IRouteFinderFactory;
import org.sokybot.persistence.service.IGamePersistenceFactory;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of IGroupContextFactory that creates group contexts.
 * 
 * Pure OSGi implementation - no Spring dependencies.
 * Creates GroupContextImpl with OSGi services injected via constructor.
 */
@Component(service = IGroupContextFactory.class)
public class GroupContextFactoryImpl implements IGroupContextFactory {

    private static final Logger log = LoggerFactory.getLogger(GroupContextFactoryImpl.class);

    private EventAdmin eventAdmin;
    private IRouteFinderFactory routeFinderFactory;
    private IGamePersistenceFactory gamePersistenceFactory;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    public void setRouteFinderFactory(IRouteFinderFactory factory) {
        this.routeFinderFactory = factory;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    public void setGamePersistenceFactory(IGamePersistenceFactory factory) {
        this.gamePersistenceFactory = factory;
    }

    @Override
    public IGroupContext createGroupContext(GroupInfo groupInfo,
            org.osgi.framework.BundleContext bundleContext) {
        log.info("Creating group context for: {} (pure OSGi, no Spring)", groupInfo.getName());

        try {
            // Direct instantiation - no Spring context
            // Services are injected via constructor and accessed via OSGi service registry
            return new GroupContextImpl(
                    groupInfo,
                    bundleContext,
                    eventAdmin,
                    routeFinderFactory,
                    gamePersistenceFactory);

        } catch (Exception e) {
            log.error("Failed to create group context for: {}", groupInfo.getName(), e);
            throw new RuntimeException("Failed to create group context: " + groupInfo.getName(), e);
        }
    }

    @Override
    public void destroyGroupContext(IGroupContext groupContext) {
        if (groupContext instanceof GroupContextImpl) {
            ((GroupContextImpl) groupContext).destroy();
        }
    }
}
