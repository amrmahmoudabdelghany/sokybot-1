package org.sokybot.bundlemanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing OSGi bundles.
 */
public class BundleManagerService {
    
    private static final Logger logger = LoggerFactory.getLogger(BundleManagerService.class);
    
    private final BundleContext bundleContext;
    
    public BundleManagerService(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
    
    /**
     * List all installed bundles.
     */
    public List<Map<String, Object>> listBundles() {
        List<Map<String, Object>> bundles = new ArrayList<>();
        
        for (Bundle bundle : bundleContext.getBundles()) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", bundle.getBundleId());
            info.put("symbolicName", bundle.getSymbolicName());
            info.put("version", bundle.getVersion().toString());
            info.put("state", getBundleStateString(bundle.getState()));
            info.put("location", bundle.getLocation());
            info.put("lastModified", bundle.getLastModified());
            bundles.add(info);
        }
        
        return bundles;
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
     * Uninstall a bundle.
     */
    public void uninstallBundle(long bundleId) throws BundleException {
        Bundle bundle = bundleContext.getBundle(bundleId);
        if (bundle == null) {
            throw new IllegalArgumentException("Bundle not found: " + bundleId);
        }
        logger.info("Uninstalling bundle: {} ({})", bundle.getSymbolicName(), bundleId);
        bundle.uninstall();
    }
    
    /**
     * Install a bundle from a location.
     */
    public Bundle installBundle(String location) throws BundleException {
        logger.info("Installing bundle from: {}", location);
        return bundleContext.installBundle(location);
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
