package org.sokybot.webview;

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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.webview.api.IBundleAdminService;

/**
 * Default implementation of {@link IBundleAdminService} running
 * inside the main webview bundle. It wraps the local
 * {@link BundleContext} and exposes a dev-tools friendly view
 * over installed bundles.
 */
@Component(service = IBundleAdminService.class, immediate = true)
public class BundleAdminServiceImpl implements IBundleAdminService {

    private static final Logger log = LoggerFactory.getLogger(BundleAdminServiceImpl.class);

    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext context) {
        this.bundleContext = context;
        log.info("BundleAdminServiceImpl activated");
    }

    @Deactivate
    public void deactivate() {
        log.info("BundleAdminServiceImpl deactivated");
    }

    @Override
    public List<Map<String, Object>> listBundles() {
        List<Map<String, Object>> bundles = new ArrayList<>();
        for (Bundle bundle : bundleContext.getBundles()) {
            bundles.add(bundleToMap(bundle));
        }
        return bundles;
    }

    @Override
    public Map<String, Object> getBundleBySymbolicName(String symbolicName) {
        Bundle bundle = findBundleBySymbolicName(symbolicName);
        if (bundle == null) {
            return null;
        }
        return bundleToMap(bundle);
    }

    @Override
    public Map<String, Object> getDevelopmentStatus(String symbolicName) {
        Bundle bundle = findBundleBySymbolicName(symbolicName);
        if (bundle == null) {
            return null;
        }

        Map<String, Object> status = new HashMap<>();
        status.put("symbolicName", bundle.getSymbolicName());
        status.put("location", bundle.getLocation());
        status.put("isWatchingForChanges", Boolean.valueOf(isWatchingForChanges(symbolicName)));
        status.put("lastModified", bundle.getLastModified());

        String location = bundle.getLocation();
        if (location != null && !location.startsWith("initial@")) {
            try {
                File bundleFile = new File(location.replace("file:", "").replace("file://", ""));
                if (bundleFile.exists()) {
                    long fileModified = bundleFile.lastModified();
                    status.put("fileModified", fileModified);
                    status.put("fileExists", Boolean.TRUE);
                    status.put("needsReload", Boolean.valueOf(fileModified > bundle.getLastModified()));
                } else {
                    status.put("fileExists", Boolean.FALSE);
                    status.put("needsReload", Boolean.FALSE);
                }
            } catch (SecurityException e) {
                status.put("fileExists", Boolean.FALSE);
                status.put("needsReload", Boolean.FALSE);
            }
        } else {
            status.put("fileExists", Boolean.FALSE);
            status.put("needsReload", Boolean.FALSE);
        }

        return status;
    }

    @Override
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

                            String importPackage = mainAttrs.getValue("Import-Package");
                            if (importPackage != null) {
                                String[] imports = importPackage.split(",");
                                for (String imp : imports) {
                                    String pkg = imp.split(";")[0].trim();
                                    if (!pkg.isEmpty()) {
                                        importedPackages.add(pkg);
                                    }
                                }
                            }

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
            log.warn("Failed to read bundle manifest: {}", bundleId, e);
        }

        deps.put("importedPackages", importedPackages);
        deps.put("exportedPackages", exportedPackages);
        deps.put("requiredBundles", requiredBundles);

        return deps;
    }

    @Override
    public void startBundle(long bundleId) throws BundleException {
        Bundle bundle = bundleContext.getBundle(bundleId);
        if (bundle == null) {
            throw new IllegalArgumentException("Bundle not found: " + bundleId);
        }
        log.info("Starting bundle: {} ({})", bundle.getSymbolicName(), Long.valueOf(bundleId));
        bundle.start();
    }

    @Override
    public void stopBundle(long bundleId) throws BundleException {
        Bundle bundle = bundleContext.getBundle(bundleId);
        if (bundle == null) {
            throw new IllegalArgumentException("Bundle not found: " + bundleId);
        }
        log.info("Stopping bundle: {} ({})", bundle.getSymbolicName(), Long.valueOf(bundleId));
        bundle.stop();
    }

    @Override
    public void restartBundle(long bundleId) throws BundleException {
        Bundle bundle = bundleContext.getBundle(bundleId);
        if (bundle == null) {
            throw new IllegalArgumentException("Bundle not found: " + bundleId);
        }
        log.info("Restarting bundle: {} ({})", bundle.getSymbolicName(), Long.valueOf(bundleId));
        bundle.stop();
        bundle.start();
    }

    @Override
    public void reloadBundleFromFileSystem(String symbolicName) throws BundleException {
        Bundle bundle = findBundleBySymbolicName(symbolicName);
        if (bundle == null) {
            throw new IllegalArgumentException("Bundle not found: " + symbolicName);
        }

        log.info("Reloading bundle from filesystem: {}", symbolicName);

        try {
            if (bundle.getState() == Bundle.ACTIVE) {
                bundle.stop();
            }
            bundle.update();
            bundle.start();
            log.info("Bundle reloaded successfully: {}", symbolicName);
        } catch (BundleException e) {
            log.error("Failed to reload bundle: {}", symbolicName, e);
            throw e;
        }
    }

    private Bundle findBundleBySymbolicName(String symbolicName) {
        for (Bundle bundle : bundleContext.getBundles()) {
            if (symbolicName.equals(bundle.getSymbolicName())) {
                return bundle;
            }
        }
        return null;
    }

    private boolean isWatchingForChanges(String symbolicName) {
        Bundle bundle = findBundleBySymbolicName(symbolicName);
        if (bundle == null) {
            return false;
        }
        String location = bundle.getLocation();
        return location != null && (location.contains("/target/") || location.contains("plugins/"));
    }

    private Map<String, Object> bundleToMap(Bundle bundle) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", Long.valueOf(bundle.getBundleId()));
        map.put("symbolicName", bundle.getSymbolicName());
        map.put("version", bundle.getVersion() != null ? bundle.getVersion().toString() : "");
        map.put("state", toState(bundle.getState()));
        map.put("location", bundle.getLocation());
        map.put("lastModified", Long.valueOf(bundle.getLastModified()));

        Map<String, Object> devStatus = getDevelopmentStatus(bundle.getSymbolicName());
        if (devStatus != null) {
            map.put("isWatchingForChanges", devStatus.get("isWatchingForChanges"));
            map.put("needsReload", devStatus.get("needsReload"));
            map.put("fileExists", devStatus.get("fileExists"));
        }

        return map;
    }

    private String toState(int state) {
        switch (state) {
        case Bundle.ACTIVE:
            return "ACTIVE";
        case Bundle.INSTALLED:
            return "INSTALLED";
        case Bundle.RESOLVED:
            return "RESOLVED";
        case Bundle.STARTING:
            return "STARTING";
        case Bundle.STOPPING:
            return "STOPPING";
        default:
            return "UNKNOWN";
        }
    }
}

