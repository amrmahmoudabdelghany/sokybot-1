package org.sokybot.machine.listener;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.settings.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Bridges OSGi events to Spring Application events for this specific machine.
 * Registers an OSGi EventHandler with a filter for the current machine's name.
 */
@Component
public class MachineEventBridge implements EventHandler {

    @Autowired
    private BundleContext bundleContext;

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private Settings settings;

    private ServiceRegistration<EventHandler> registration;

    @PostConstruct
    public void init() {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(EventConstants.EVENT_TOPIC, "sokybot/game/*");
        
        // Filter events for this specific machine
        String filter = "(fullName=" + getMachineFullName() + ")";
        props.put(EventConstants.EVENT_FILTER, filter);

        registration = bundleContext.registerService(EventHandler.class, this, props);
    }

    @PreDestroy
    public void cleanup() {
        if (registration != null) {
            registration.unregister();
        }
    }

    @Override
    public void handleEvent(Event event) {
        Object payload = event.getProperty("event");
        if (payload instanceof IGameEvent) {
            // Publish to local Spring context
            eventPublisher.publishEvent(payload);
        }
    }
    
    private String getMachineFullName() {
       return settings.getMachineName(); // Assuming Settings has this or similar
    }
}
