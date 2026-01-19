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
// @Component(immediate = true) // Removed: DevToolsRSocketHandler is now a component
public class DevToolsActivator implements BundleActivator {
    
    private static final Logger logger = LoggerFactory.getLogger(DevToolsActivator.class);
    
    private BundleContext bundleContext;
    private DevToolsHttpServer httpServer;
    
    private static final int HTTP_PORT = 7001;
    
    @Activate
    public void activate(ComponentContext componentContext) {
        this.bundleContext = componentContext.getBundleContext();
        startInternal();
    }
    
    @Override
    public void start(BundleContext context) throws Exception {
        this.bundleContext = context;
        startInternal();
    }

    private void startInternal() {
        logger.info("DevTools bundle activating...");
        
        try {
            // Determine webapp path
            Path webappPath = getWebappPath();
            
            // Start HTTP server
            // Note: services are now initialized by DevToolsRSocketHandler component
            this.httpServer = new DevToolsHttpServer(HTTP_PORT, webappPath);
            httpServer.start();
            
            logger.info("========================================");
            logger.info("Dev Tools Bundle Activated Successfully");
            logger.info("========================================");
            logger.info("HTTP Server: http://localhost:{}/devtools", HTTP_PORT);
            logger.info("========================================");
            
        } catch (Exception e) {
            logger.error("Failed to activate DevTools bundle", e);
            throw new RuntimeException("Failed to activate DevTools", e);
        }
    }
    
    @Deactivate
    public void deactivate() {
        stopInternal();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        stopInternal();
    }
    
    private void stopInternal() {
        logger.info("DevTools bundle deactivating...");
        
        try {
            if (httpServer != null) {
                httpServer.stop();
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
    
}
