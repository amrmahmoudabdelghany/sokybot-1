package org.sokybot.groupui;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.ContextLifecycleEvents;
import org.sokybot.machinegroup.PageContainer;
import org.sokybot.machinegroup.navigationtree.INavTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event handler that listens to group context lifecycle events and manages group UI components.
 */
@Component(
    immediate = true,
    service = EventHandler.class,
    property = {
        EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_GROUP_CONTEXT_CREATED,
        EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_GROUP_CONTEXT_DESTROYED
    }
)
public class GroupUIEventHandler implements EventHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GroupUIEventHandler.class);
    
    @Reference
    private PageContainer pageContainer;
    
    @Reference
    private INavTree navTree;
    
    // Track UI instances per group (groupName -> GroupUIInstance)
    private final Map<String, GroupUIInstance> groupUIs = new ConcurrentHashMap<>();

    @Override
    public void handleEvent(Event event) {
        String topic = event.getTopic();
        log.debug("Received group context lifecycle event: {}", topic);
        
        try {
            if (topic.equals(ContextLifecycleEvents.TOPIC_GROUP_CONTEXT_CREATED)) {
                handleGroupCreated(event);
            } else if (topic.equals(ContextLifecycleEvents.TOPIC_GROUP_CONTEXT_DESTROYED)) {
                handleGroupDestroyed(event);
            } else {
                log.warn("Unknown event topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("Error handling context lifecycle event: {}", topic, e);
        }
    }
    
    private void handleGroupCreated(Event event) {
        String groupName = (String) event.getProperty(ContextLifecycleEvents.PROP_GROUP_NAME);
        IGroupContext context = (IGroupContext) event.getProperty(ContextLifecycleEvents.PROP_CONTEXT);
        
        if (context == null) {
            log.error("Group context is null in CREATED event for {}", groupName);
            return;
        }
        
        log.info("Creating UI for group: {}", groupName);
        
        try {
            GroupUIInstance ui = new GroupUIInstance(
                groupName,
                context,
                pageContainer,
                navTree
            );
            
            ui.createPages();
            groupUIs.put(groupName, ui);
            
            log.info("Successfully created UI for group: {}", groupName);
        } catch (Exception e) {
            log.error("Failed to create UI for group: {}", groupName, e);
        }
    }
    
    private void handleGroupDestroyed(Event event) {
        String groupName = (String) event.getProperty(ContextLifecycleEvents.PROP_GROUP_NAME);
        
        log.info("Destroying UI for group: {}", groupName);
        
        GroupUIInstance ui = groupUIs.remove(groupName);
        if (ui != null) {
            try {
                ui.destroyPages();
                log.info("Successfully destroyed UI for group: {}", groupName);
            } catch (Exception e) {
                log.error("Error destroying UI for group: {}", groupName, e);
            }
        } else {
            log.warn("No UI instance found for group: {}", groupName);
        }
    }
}
