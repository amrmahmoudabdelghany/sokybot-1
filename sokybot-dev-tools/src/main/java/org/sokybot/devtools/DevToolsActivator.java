package org.sokybot.devtools;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Bundle activator for dev-tools bundle.
 * Initializes all services and starts HTTP and RSocket servers.
 */
@Component(immediate = true)
public class DevToolsActivator implements BundleActivator {
    
    private static final Logger logger = LoggerFactory.getLogger(DevToolsActivator.class);
    
    private BundleContext bundleContext;
    private BundleManagementService bundleService;
    private ServiceInspectionService serviceService;
    private RuntimeMetricsService metricsService;
    private LogStreamingService logService;
    private DevToolsRSocketHandler rsocketHandler;
    private DevToolsHttpServer httpServer;
    private ServiceRegistration<DevToolsRSocketHandler> rsocketHandlerRegistration;
    
    private static final int RSOCKET_PORT = 7002; // Different from main webview RSocket (7000)
    private static final int HTTP_PORT = 7001;
    
    @Activate
    public void activate(ComponentContext componentContext) {
        this.bundleContext = componentContext.getBundleContext();
        logger.info("DevTools bundle activating...");
        
        try {
            // Initialize services
            this.bundleService = new BundleManagementService(bundleContext);
            this.serviceService = new ServiceInspectionService(bundleContext);
            this.metricsService = new RuntimeMetricsService();
            this.logService = new LogStreamingService();
            
            // Create RSocket handler
            this.rsocketHandler = new DevToolsRSocketHandler(
                bundleService, serviceService, metricsService, logService);
            
            // Register RSocket handler as OSGi service (for listeners)
            this.rsocketHandlerRegistration = bundleContext.registerService(
                DevToolsRSocketHandler.class, rsocketHandler, null);
            
            // Start RSocket server
            rsocketHandler.start(RSOCKET_PORT);
            
            // Determine webapp path
            Path webappPath = getWebappPath();
            
            // Start HTTP server
            this.httpServer = new DevToolsHttpServer(HTTP_PORT, webappPath);
            httpServer.start();
            
            logger.info("========================================");
            logger.info("Dev Tools Bundle Activated Successfully");
            logger.info("========================================");
            logger.info("HTTP Server: http://localhost:{}/devtools", HTTP_PORT);
            logger.info("RSocket Server: ws://localhost:{}", RSOCKET_PORT);
            logger.info("========================================");
            
        } catch (Exception e) {
            logger.error("Failed to activate DevTools bundle", e);
            throw new RuntimeException("Failed to activate DevTools", e);
        }
    }
    
    @Deactivate
    public void deactivate() {
        logger.info("DevTools bundle deactivating...");
        
        try {
            if (rsocketHandlerRegistration != null) {
                rsocketHandlerRegistration.unregister();
            }
            
            if (httpServer != null) {
                httpServer.stop();
            }
            
            if (rsocketHandler != null) {
                rsocketHandler.stop();
            }
            
            if (logService != null) {
                logService.stop();
            }
            
            logger.info("DevTools bundle deactivated");
        } catch (Exception e) {
            logger.error("Error during deactivation", e);
        }
    }
    
    /**
     * Get webapp path for serving React app.
     * Checks bundle resources first (when deployed), then target/webapp (development).
     */
    private Path getWebappPath() {
        // First, try to get webapp from bundle resources (when deployed)
        try {
            java.net.URL webappUrl = bundleContext.getBundle().getResource("webapp");
            if (webappUrl != null) {
                String urlString = webappUrl.toString();
                if (urlString.startsWith("file:")) {
                    // Extract file path
                    String filePath = urlString.replaceFirst("file:", "").replaceFirst("!/webapp", "").replaceFirst("!/", "");
                    // Handle bundle file URLs
                    if (filePath.contains("!")) {
                        filePath = filePath.substring(0, filePath.indexOf("!"));
                    }
                    Path webappPath = Paths.get(filePath, "webapp");
                    if (webappPath.toFile().exists() && webappPath.toFile().isDirectory()) {
                        logger.info("Using webapp path from bundle: {}", webappPath);
                        return webappPath;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not get webapp from bundle resources", e);
        }
        
        // Try target/webapp (development/build time)
        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            Path targetWebapp = Paths.get(userDir, "sokybot-dev-tools", "target", "webapp");
            if (targetWebapp.toFile().exists() && targetWebapp.toFile().isDirectory()) {
                logger.info("Using webapp path from target: {}", targetWebapp);
                return targetWebapp;
            }
            
            // Also try without sokybot-dev-tools prefix (if running from root)
            Path altTargetWebapp = Paths.get(userDir, "target", "webapp");
            if (altTargetWebapp.toFile().exists() && altTargetWebapp.toFile().isDirectory()) {
                logger.info("Using webapp path from alt target: {}", altTargetWebapp);
                return altTargetWebapp;
            }
        }
        
        // Fallback: try to extract from bundle location
        try {
            String bundleLocation = bundleContext.getBundle().getLocation();
            if (bundleLocation != null && bundleLocation.startsWith("file:")) {
                String bundlePath = bundleLocation.replace("file:", "");
                Path bundleDir = Paths.get(bundlePath).getParent();
                if (bundleDir != null) {
                    // Try to find webapp in common locations
                    Path[] possiblePaths = {
                        bundleDir.resolve("webapp"),
                        bundleDir.resolve("target").resolve("webapp"),
                        bundleDir.getParent().resolve("target").resolve("webapp")
                    };
                    for (Path path : possiblePaths) {
                        if (path.toFile().exists() && path.toFile().isDirectory()) {
                            logger.info("Using webapp path: {}", path);
                            return path;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract webapp path from bundle location", e);
        }
        
        // Default: use current working directory + target/webapp
        Path defaultPath = Paths.get("target", "webapp");
        logger.warn("Webapp directory not found, using default: {}", defaultPath);
        defaultPath.toFile().mkdirs();
        return defaultPath;
    }
    
    // BundleActivator methods (for compatibility if needed)
    
    @Override
    public void start(BundleContext context) throws Exception {
        // Already handled by @Activate
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        // Already handled by @Deactivate
    }
    
    // Getters for services (useful for testing or other bundles)
    
    public DevToolsRSocketHandler getRSocketHandler() {
        return rsocketHandler;
    }
    
    public BundleManagementService getBundleService() {
        return bundleService;
    }
    
    public ServiceInspectionService getServiceService() {
        return serviceService;
    }
    
    public RuntimeMetricsService getMetricsService() {
        return metricsService;
    }
}
