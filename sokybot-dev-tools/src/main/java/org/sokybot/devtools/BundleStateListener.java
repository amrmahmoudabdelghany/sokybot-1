package org.sokybot.devtools;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens for bundle state changes and notifies the frontend via RSocket stream.
 */
@Component(immediate = true)
public class BundleStateListener implements BundleListener {
    
    private static final Logger logger = LoggerFactory.getLogger(BundleStateListener.class);
    
    private BundleContext bundleContext;
    
    @Reference(
        bind = "setRSocketHandler", 
        unbind = "unsetRSocketHandler",
        cardinality = org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY,
        policy = org.osgi.service.component.annotations.ReferencePolicy.STATIC
    )
    private volatile DevToolsRSocketHandler rsocketHandler;
    
    public void setRSocketHandler(DevToolsRSocketHandler handler) {
        this.rsocketHandler = handler;
    }
    
    public void unsetRSocketHandler(DevToolsRSocketHandler handler) {
        this.rsocketHandler = null;
    }
    
    @Activate
    public void activate(org.osgi.service.component.ComponentContext context) {
        this.bundleContext = context.getBundleContext();
        this.bundleContext.addBundleListener(this);
        logger.info("BundleStateListener activated");
    }
    
    @Deactivate
    public void deactivate() {
        if (bundleContext != null) {
            bundleContext.removeBundleListener(this);
        }
        logger.info("BundleStateListener deactivated");
    }
    
    @Override
    public void bundleChanged(BundleEvent event) {
        if (rsocketHandler == null) {
            return;
        }
        
        Bundle bundle = event.getBundle();
        int state = bundle.getState();
        int eventType = event.getType();
        
        logger.debug("Bundle state changed: {} ({}), event type: {}", 
            bundle.getSymbolicName(), getBundleStateString(state), eventType);
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("type", "bundle.changed");
        eventData.put("bundleId", bundle.getBundleId());
        eventData.put("symbolicName", bundle.getSymbolicName());
        eventData.put("state", getBundleStateString(state));
        eventData.put("eventType", getEventTypeString(eventType));
        eventData.put("timestamp", System.currentTimeMillis());
        
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
    
    private String getEventTypeString(int eventType) {
        switch (eventType) {
            case BundleEvent.INSTALLED: return "INSTALLED";
            case BundleEvent.STARTED: return "STARTED";
            case BundleEvent.STOPPED: return "STOPPED";
            case BundleEvent.UPDATED: return "UPDATED";
            case BundleEvent.UNINSTALLED: return "UNINSTALLED";
            case BundleEvent.RESOLVED: return "RESOLVED";
            case BundleEvent.UNRESOLVED: return "UNRESOLVED";
            case BundleEvent.STARTING: return "STARTING";
            case BundleEvent.STOPPING: return "STOPPING";
            case BundleEvent.LAZY_ACTIVATION: return "LAZY_ACTIVATION";
            default: return "UNKNOWN";
        }
    }
}
