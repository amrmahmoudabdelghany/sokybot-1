package org.sokybot.webview;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class WebviewActivator implements BundleActivator {
    
    private static final Logger logger = LoggerFactory.getLogger(WebviewActivator.class);
    private static final int HTTP_PORT = 7003;
    
    private WebviewHttpServer httpServer;
    private BundleContext bundleContext;

    @Override
    public void start(BundleContext context) throws Exception {
        this.bundleContext = context;
        logger.info("Starting Webview Bundle (Activator)...");
        
        try {
            // Determine webapp path
            Path webappPath = getWebappPath();
            logger.info("Webview serving from: {}", webappPath);
            
            // Start HTTP server
            this.httpServer = new WebviewHttpServer(HTTP_PORT, webappPath);
            httpServer.start();
            
            logger.info("Webview HTTP Server accessible at http://localhost:{}/", HTTP_PORT);
        } catch (Exception e) {
            logger.error("Failed to start Webview HTTP Server", e);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        logger.info("Stopping Webview Bundle (Activator)...");
        if (httpServer != null) {
            httpServer.stop();
        }
    }
    
    /**
     * Get webapp path for serving React app.
     * Checks bundle resources first (when deployed), then target/frontend/dist (development).
     */
    private Path getWebappPath() {
        // First, try to get webapp from bundle resources (when deployed)
        try {
            java.net.URL webappUrl = bundleContext.getBundle().getResource("static");
            if (webappUrl != null) {
                String urlString = webappUrl.toString();
                if (urlString.startsWith("file:")) {
                    // Extract file path
                    String filePath = urlString.replaceFirst("file:", "").replaceFirst("!/static", "").replaceFirst("!/", "");
                    // Handle bundle file URLs
                    if (filePath.contains("!")) {
                        filePath = filePath.substring(0, filePath.indexOf("!"));
                    }
                    Path webappPath = Paths.get(filePath, "static");
                    if (webappPath.toFile().exists() && webappPath.toFile().isDirectory()) {
                        return webappPath;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not get webapp from bundle resources", e);
        }
        
        // Try local development paths
        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            // Check sokybot-webview/target/frontend/dist
            Path targetWebapp = Paths.get(userDir, "sokybot-webview", "target", "frontend", "dist");
            if (targetWebapp.toFile().exists() && targetWebapp.toFile().isDirectory()) {
                return targetWebapp;
            }
            
            // Check target/frontend/dist (if running from module root)
            Path altTargetWebapp = Paths.get(userDir, "target", "frontend", "dist");
            if (altTargetWebapp.toFile().exists() && altTargetWebapp.toFile().isDirectory()) {
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
                        bundleDir.resolve("static"),
                        bundleDir.resolve("target").resolve("frontend").resolve("dist"),
                        bundleDir.getParent().resolve("target").resolve("frontend").resolve("dist")
                    };
                    for (Path path : possiblePaths) {
                        if (path.toFile().exists() && path.toFile().isDirectory()) {
                            return path;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract webapp path from bundle location", e);
        }
        
        // Default: use current working directory + target/frontend/dist
        Path defaultPath = Paths.get("target", "frontend", "dist");
        return defaultPath;
    }
}
