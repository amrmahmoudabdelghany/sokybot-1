package org.sokybot.webview.api;

import java.util.List;
import java.util.Map;

/**
 * Admin service for inspecting and controlling OSGi bundles.
 * <p>
 * This is a small, UI-facing façade so Groovy pages and other
 * components can query bundle state and perform basic lifecycle
 * operations without directly depending on {@code BundleContext}.
 */
public interface IBundleAdminService {

    /**
     * List all installed bundles with dev-tools friendly metadata.
     */
    List<Map<String, Object>> listBundles();

    /**
     * Get bundle information by symbolic name.
     */
    Map<String, Object> getBundleBySymbolicName(String symbolicName);

    /**
     * Get development status for a bundle (watching for changes, needs reload, etc.).
     */
    Map<String, Object> getDevelopmentStatus(String symbolicName);

    /**
     * Get bundle dependencies by bundle id.
     */
    Map<String, Object> getBundleDependencies(long bundleId);

    /**
     * Start bundle.
     */
    void startBundle(long bundleId) throws Exception;

    /**
     * Stop bundle.
     */
    void stopBundle(long bundleId) throws Exception;

    /**
     * Restart bundle.
     */
    void restartBundle(long bundleId) throws Exception;

    /**
     * Reload bundle from filesystem (if supported by the environment).
     */
    void reloadBundleFromFileSystem(String symbolicName) throws Exception;
}

