package org.sokybot.bundlemanager;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.webview.api.IWebviewConfigurator;
import org.sokybot.webview.api.util.SchemaLoader;

/**
 * OSGi Declarative Services component that registers a "Bundle Manager" toolbar action
 * in the webview, allowing users to manage installed OSGi bundles.
 */
@Component(immediate = true)
public class BundleManagerActivator implements BundleListener {
    
    private static final Logger logger = LoggerFactory.getLogger(BundleManagerActivator.class);
    
    private static final String ACTION_ID = "bundle-manager";
    
    @Reference
    private IWebviewConfigurator webviewConfigurator;
    
    private BundleContext bundleContext;
    private BundleManagerService bundleService;
    
    @Activate
    public void activate(ComponentContext componentContext) {
        logger.info("BundleManagerActivator: Starting Bundle Manager");
        this.bundleContext = componentContext.getBundleContext();
        this.bundleService = new BundleManagerService(bundleContext);
        
        // Load modal schema
        Map<String, Object> modalSchema = loadSchema();
        
        // Register toolbar action
        webviewConfigurator.addToolbarAction(
            ACTION_ID,
            "Bundle Manager",
            "Package",
            modalSchema
        );
        
        // Register action handler
        webviewConfigurator.registerToolbarActionHandler(ACTION_ID, this::handleAction);
        
        // Listen for bundle events
        bundleContext.addBundleListener(this);
        
        logger.info("BundleManagerActivator: Bundle Manager registered");
    }
    
    @Deactivate
    public void deactivate() {
        logger.info("BundleManagerActivator: Stopping Bundle Manager");
        
        if (bundleContext != null) {
            bundleContext.removeBundleListener(this);
        }
        
        if (webviewConfigurator != null) {
            webviewConfigurator.removeToolbarAction(ACTION_ID);
        }
        
        logger.info("BundleManagerActivator: Bundle Manager unregistered");
    }
    
    @Override
    public void bundleChanged(BundleEvent event) {
        // Notify frontend about bundle changes
        if (webviewConfigurator != null) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("type", "bundle.changed");
            eventData.put("bundleId", event.getBundle().getBundleId());
            eventData.put("bundleState", getBundleStateString(event.getBundle().getState()));
            webviewConfigurator.sendEvent("bundle.manager", eventData);
        }
    }
    
    private Map<String, Object> loadSchema() {
        try {
            return SchemaLoader.loadSchema("/ui/bundles.json", getClass());
        } catch (Exception e) {
            logger.warn("Failed to load bundles.json schema, using empty schema", e);
            return new HashMap<>();
        }
    }
    
    private Map<String, Object> handleAction(String action, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            switch (action) {
                case "init":
                case "refresh":
                    response.put("success", true);
                    response.put("state", Map.of("bundles", bundleService.listBundles()));
                    break;
                    
                case "start":
                    long startId = ((Number) data.get("bundleId")).longValue();
                    bundleService.startBundle(startId);
                    response.put("success", true);
                    response.put("state", Map.of("bundles", bundleService.listBundles()));
                    break;
                    
                case "stop":
                    long stopId = ((Number) data.get("bundleId")).longValue();
                    bundleService.stopBundle(stopId);
                    response.put("success", true);
                    response.put("state", Map.of("bundles", bundleService.listBundles()));
                    break;
                    
                case "restart":
                    long restartId = ((Number) data.get("bundleId")).longValue();
                    bundleService.restartBundle(restartId);
                    response.put("success", true);
                    response.put("state", Map.of("bundles", bundleService.listBundles()));
                    break;
                    
                case "uninstall":
                    long uninstallId = ((Number) data.get("bundleId")).longValue();
                    bundleService.uninstallBundle(uninstallId);
                    response.put("success", true);
                    response.put("state", Map.of("bundles", bundleService.listBundles()));
                    break;
                    
                case "install":
                    String location = (String) data.get("location");
                    Bundle installed = bundleService.installBundle(location);
                    response.put("success", true);
                    response.put("installedBundleId", installed.getBundleId());
                    response.put("state", Map.of("bundles", bundleService.listBundles()));
                    break;
                    
                default:
                    response.put("success", false);
                    response.put("error", "Unknown action: " + action);
            }
        } catch (Exception e) {
            logger.error("Error handling action: " + action, e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return response;
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
