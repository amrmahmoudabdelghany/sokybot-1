package org.sokybot.devtools;

import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens for FileInstall events and notifies the frontend via RSocket stream.
 */
@Component(
    immediate = true,
    service = EventHandler.class,
    property = {
        EventConstants.EVENT_TOPIC + "=org/osgi/service/fileinstall/FILEINSTALL_BUNDLE_INSTALLED",
        EventConstants.EVENT_TOPIC + "=org/osgi/service/fileinstall/FILEINSTALL_BUNDLE_UPDATED",
        EventConstants.EVENT_TOPIC + "=org/osgi/service/fileinstall/FILEINSTALL_BUNDLE_UNINSTALLED"
    }
)
public class FileInstallListener implements EventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(FileInstallListener.class);
    
    private volatile DevToolsRSocketHandler rsocketHandler;
    
    @Reference
    protected void setRSocketHandler(DevToolsRSocketHandler handler) {
        this.rsocketHandler = handler;
    }
    
    protected void unsetRSocketHandler(DevToolsRSocketHandler handler) {
        this.rsocketHandler = null;
    }
    
    @Override
    public void handleEvent(Event event) {
        if (rsocketHandler == null) {
            return;
        }
        
        String topic = event.getTopic();
        String location = (String) event.getProperty("fileinstall.bundle.location");
        Long bundleId = (Long) event.getProperty("fileinstall.bundle.id");
        
        logger.info("FileInstall event: {} for bundle: {}", topic, location);
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("type", "fileinstall");
        eventData.put("topic", topic);
        eventData.put("location", location);
        eventData.put("bundleId", bundleId);
        eventData.put("timestamp", System.currentTimeMillis());
        
        // Extract bundle symbolic name if possible
        if (bundleId != null) {
            // Try to get bundle from event properties or context
            Bundle bundle = (Bundle) event.getProperty("bundle");
            if (bundle != null) {
                eventData.put("symbolicName", bundle.getSymbolicName());
                eventData.put("state", getBundleStateString(bundle.getState()));
            }
        }
        
        // Emit to RSocket stream
        rsocketHandler.emitBundleEvent(eventData);
    }
    
    private String getBundleStateString(int state) {
        switch (state) {
            case Bundle.UNINSTALLED: return "UNINSTALLED";
            case Bundle.INSTALLED: return "INSTALLED";
            case Bundle.RESOLVED: return "RESOLVED";
            case Bundle.STARTING: return "STARTING";
            case Bundle.STOPPING: return "STOPPING";
            case Bundle.ACTIVE: return "ACTIVE";
            default: return "UNKNOWN";
        }
    }
}
