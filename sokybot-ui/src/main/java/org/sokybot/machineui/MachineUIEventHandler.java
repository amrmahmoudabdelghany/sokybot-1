package org.sokybot.machineui;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.osgi.framework.BundleContext;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.ContextLifecycleEvents;
import org.sokybot.machinegroup.DashboardContainer;
import org.sokybot.machinegroup.PageContainer;
import org.sokybot.machinegroup.navigationtree.INavTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event handler that listens to context lifecycle events and manages machine UI components.
 * 
 * When a machine context is created, this handler creates and registers all UI pages
 * and dashboards for that machine. When destroyed, it removes them.
 */
@Component(
    immediate = true,
    service = EventHandler.class,
    property = {
        EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED,
        EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_DESTROYED
    }
)
public class MachineUIEventHandler implements EventHandler {
    
    private static final Logger log = LoggerFactory.getLogger(MachineUIEventHandler.class);
    
    @Reference
    private PageContainer pageContainer;
    
    @Reference
    private DashboardContainer dashboardContainer;
    
    @Reference
    private INavTree navTree;
    
    // Track UI instances per machine (fullName -> MachineUIInstance)
    private final Map<String, MachineUIInstance> machineUIs = new ConcurrentHashMap<>();

    private BundleContext bundleContext;

    @org.osgi.service.component.annotations.Activate
    public void activate(org.osgi.framework.BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
    
    @Override
    public void handleEvent(Event event) {
        String topic = event.getTopic();
        log.debug("Received context lifecycle event: {}", topic);
        
        try {
            if (topic.equals(ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED)) {
                handleMachineCreated(event);
            } else if (topic.equals(ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_DESTROYED)) {
                handleMachineDestroyed(event);
            } else {
                log.warn("Unknown event topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("Error handling context lifecycle event: {}", topic, e);
        }
    }
    
    private void handleMachineCreated(Event event) {
        String groupName = (String) event.getProperty(ContextLifecycleEvents.PROP_GROUP_NAME);
        String machineName = (String) event.getProperty(ContextLifecycleEvents.PROP_MACHINE_NAME);
        String fullName = (String) event.getProperty(ContextLifecycleEvents.PROP_FULL_NAME);
        IMachineContext context = (IMachineContext) event.getProperty(ContextLifecycleEvents.PROP_CONTEXT);
        
        if (context == null) {
            log.error("Machine context is null in CREATED event for {}", fullName);
            return;
        }
        
        log.info("Creating UI for machine: {}", fullName);
        
        try {
            MachineUIInstance ui = new MachineUIInstance(
                groupName, 
                machineName, 
                context,
                pageContainer,
                dashboardContainer,
                navTree,
                bundleContext
            );
            
            ui.createPages();
            machineUIs.put(fullName, ui);
            
            log.info("Successfully created UI for machine: {}", fullName);
        } catch (Exception e) {
            log.error("Failed to create UI for machine: {}", fullName, e);
        }
    }
    
    private void handleMachineDestroyed(Event event) {
        String fullName = (String) event.getProperty(ContextLifecycleEvents.PROP_FULL_NAME);
        
        log.info("Destroying UI for machine: {}", fullName);
        
        MachineUIInstance ui = machineUIs.remove(fullName);
        if (ui != null) {
            try {
                ui.destroyPages();
                log.info("Successfully destroyed UI for machine: {}", fullName);
            } catch (Exception e) {
                log.error("Error destroying UI for machine: {}", fullName, e);
            }
        } else {
            log.warn("No UI instance found for machine: {}", fullName);
        }
    }
}
