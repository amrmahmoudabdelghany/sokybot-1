package org.sokybot.devtools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing OSGi bundles with development-specific features.
 */
public class BundleManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(BundleManagementService.class);
    
    private final BundleContext bundleContext;
    
    public BundleManagementService(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
    
    /**
     * List all installed bundles.
     */
    public List<Map<String, Object>> listBundles() {
        List<Map<String, Object>> bundles = new ArrayList<>();
        
        for (Bundle bundle : bundleContext.getBundles()) {
            bundles.add(bundleToMap(bundle));
        }
        
        return bundles;
    }
    
    /**
     * Get bundle information by symbolic name.
     */
    public Map<String, Object> getBundleBySymbolicName(String symbolicName) {
        Bundle bundle = findBundleBySymbolicName(symbolicName);
        if (bundle == null) {
            return null;
        }
        return bundleToMap(bundle);
    }
    
    /**
     * Find bundle by symbolic name.
     */
    public Bundle findBundleBySymbolicName(String symbolicName) {
        for (Bundle bundle : bundleContext.getBundles()) {
            if (symbolicName.equals(bundle.getSymbolicName())) {
                return bundle;
            }
        }
        return null;
    }
    
    /**
     * Check if bundle is watching for file changes (development mode).
     */
    public boolean isWatchingForChanges(String symbolicName) {
        Bundle bundle = findBundleBySymbolicName(symbolicName);
        if (bundle == null) {
            return false;
        }
        String location = bundle.getLocation();
        return location != null && (location.contains("/target/") || location.contains("plugins/"));
    }
    
    /**
     * Get development status for a bundle.
     */
    public Map<String, Object> getDevelopmentStatus(String symbolicName) {
        Bundle bundle = findBundleBySymbolicName(symbolicName);
        if (bundle == null) {
            return null;
        }
        
        Map<String, Object> status = new HashMap<>();
        status.put("symbolicName", bundle.getSymbolicName());
        status.put("location", bundle.getLocation());
        status.put("isWatchingForChanges", isWatchingForChanges(symbolicName));
        status.put("lastModified", bundle.getLastModified());
        
        // Check if file exists and if there's a newer version
        String location = bundle.getLocation();
        if (location != null && !location.startsWith("initial@")) {
            try {
                File bundleFile = new File(location.replace("file:", "").replace("file://", ""));
                if (bundleFile.exists()) {
                    long fileModified = bundleFile.lastModified();
                    status.put("fileModified", fileModified);
                    status.put("fileExists", true);
                    status.put("needsReload", fileModified > bundle.getLastModified());
                } else {
                    status.put("fileExists", false);
                    status.put("needsReload", false);
                }
            } catch (Exception e) {
                status.put("fileExists", false);
                status.put("needsReload", false);
            }
        } else {
            status.put("fileExists", false);
            status.put("needsReload", false);
        }
        
        return status;
    }
    
    /**
     * Reload bundle from file system (development hot-reload).
     */
    public void reloadBundleFromFileSystem(String symbolicName) throws BundleException {
        Bundle bundle = findBundleBySymbolicName(symbolicName);
        if (bundle == null) {
            throw new IllegalArgumentException("Bundle not found: " + symbolicName);
        }
        
        logger.info("Reloading bundle from filesystem: {}", symbolicName);
        
        try {
            // Stop bundle
            if (bundle.getState() == Bundle.ACTIVE) {
                bundle.stop();
            }
            
            // Update bundle (triggers FileInstall if configured)
            bundle.update();
            
            // Start bundle
            bundle.start();
            
            logger.info("Bundle reloaded successfully: {}", symbolicName);
        } catch (BundleException e) {
            logger.error("Failed to reload bundle: " + symbolicName, e);
            throw e;
        }
    }
    
    /**
     * Get bundle dependencies from MANIFEST.
     */
    public Map<String, Object> getBundleDependencies(long bundleId) {
        Bundle bundle = bundleContext.getBundle(bundleId);
        if (bundle == null) {
            throw new IllegalArgumentException("Bundle not found: " + bundleId);
        }
        
        Map<String, Object> deps = new HashMap<>();
        List<String> importedPackages = new ArrayList<>();
        List<String> exportedPackages = new ArrayList<>();
        List<String> requiredBundles = new ArrayList<>();
        
        try {
            String location = bundle.getLocation();
            if (location != null && !location.startsWith("initial@")) {
                String filePath = location.replace("file:", "").replace("file://", "");
                File bundleFile = new File(filePath);
                
                if (bundleFile.exists() && bundleFile.getName().endsWith(".jar")) {
                    try (JarFile jarFile = new JarFile(bundleFile)) {
                        Manifest manifest = jarFile.getManifest();
                        if (manifest != null) {
                            Attributes mainAttrs = manifest.getMainAttributes();
                            
                            // Parse Import-Package
                            String importPackage = mainAttrs.getValue("Import-Package");
                            if (importPackage != null) {
                                // Simple parsing - split by comma and extract package names
                                String[] imports = importPackage.split(",");
                                for (String imp : imports) {
                                    String pkg = imp.split(";")[0].trim();
                                    if (!pkg.isEmpty()) {
                                        importedPackages.add(pkg);
                                    }
                                }
                            }
                            
                            // Parse Export-Package
                            String exportPackage = mainAttrs.getValue("Export-Package");
                            if (exportPackage != null) {
                                String[] exports = exportPackage.split(",");
                                for (String exp : exports) {
                                    String pkg = exp.split(";")[0].trim();
                                    if (!pkg.isEmpty()) {
                                        exportedPackages.add(pkg);
                                    }
                                }
                            }
                            
                            // Parse Require-Bundle
                            String requireBundle = mainAttrs.getValue("Require-Bundle");
                            if (requireBundle != null) {
                                String[] requires = requireBundle.split(",");
                                for (String req : requires) {
                                    String bundleName = req.split(";")[0].trim();
                                    if (!bundleName.isEmpty()) {
                                        requiredBundles.add(bundleName);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to read bundle manifest: " + bundleId, e);
        }
        
        deps.put("importedPackages", importedPackages);
        deps.put("exportedPackages", exportedPackages);
        deps.put("requiredBundles", requiredBundles);
        
        return deps;
    }
    
    /**
     * Start a bundle.
     */
    public void startBundle(long bundleId) throws BundleException {
        Bundle bundle = bundleContext.getBundle(bundleId);
        if (bundle == null) {
            throw new IllegalArgumentException("Bundle not found: " + bundleId);
        }
        logger.info("Starting bundle: {} ({})", bundle.getSymbolicName(), bundleId);
        bundle.start();
    }
    
    /**
     * Stop a bundle.
     */
    public void stopBundle(long bundleId) throws BundleException {
        Bundle bundle = bundleContext.getBundle(bundleId);
        if (bundle == null) {
            throw new IllegalArgumentException("Bundle not found: " + bundleId);
        }
        logger.info("Stopping bundle: {} ({})", bundle.getSymbolicName(), bundleId);
        bundle.stop();
    }
    
    /**
     * Restart a bundle (stop then start).
     */
    public void restartBundle(long bundleId) throws BundleException {
        Bundle bundle = bundleContext.getBundle(bundleId);
        if (bundle == null) {
            throw new IllegalArgumentException("Bundle not found: " + bundleId);
        }
        logger.info("Restarting bundle: {} ({})", bundle.getSymbolicName(), bundleId);
        bundle.stop();
        bundle.start();
    }
    
    /**
     * Convert bundle to map for JSON serialization.
     */
    private Map<String, Object> bundleToMap(Bundle bundle) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", bundle.getBundleId());
        info.put("symbolicName", bundle.getSymbolicName());
        info.put("version", bundle.getVersion().toString());
        info.put("state", getBundleStateString(bundle.getState()));
        info.put("location", bundle.getLocation());
        info.put("lastModified", bundle.getLastModified());
        
        // Add development status
        Map<String, Object> devStatus = getDevelopmentStatus(bundle.getSymbolicName());
        if (devStatus != null) {
            info.put("isWatchingForChanges", devStatus.get("isWatchingForChanges"));
            info.put("needsReload", devStatus.get("needsReload"));
            info.put("fileExists", devStatus.get("fileExists"));
        }
        
        return info;
    }
    
    /**
     * Get bundle state as string.
     */
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
