package org.sokybot;

import org.apache.felix.framework.Felix;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Smart OSGi Bootstrap Launcher for Sokybot.
 * This class is responsible for initializing the OSGi framework and loading bundles in a specific order.
 */
public class SokybotLauncher {

    private Framework framework;

    public static void main(String[] args) {
        // Platform tweaks for VMware/Remote Desktop
        System.setProperty("sun.java2d.noddraw", "true");
        System.setProperty("sun.java2d.d3d", "false");
        System.setProperty("sun.java2d.opengl", "false");
        System.setProperty("sun.java2d.pmoffscreen", "false");
        
        System.out.println("========================================");
        System.out.println("      SOKYBOT SMART BOOTSTRAP v4.0      ");
        System.out.println("========================================");

        try {
            new SokybotLauncher().launch();
        } catch (Exception e) {
            System.err.println("FATAL ERROR: Failed to launch Sokybot!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void launch() throws Exception {
        // Prepare directories
        java.io.File pluginsDir = new java.io.File("plugins");
        if (!pluginsDir.exists()) pluginsDir.mkdirs();

        Map<String, String> config = new HashMap<>();
        config.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        config.put(Constants.FRAMEWORK_STORAGE, "felix-cache");
        
        // FileInstall Configuration
        config.put("felix.fileinstall.dir", "./plugins");
        config.put("felix.fileinstall.poll", "2000"); // Poll every 2 seconds
        config.put("felix.fileinstall.noInitialDelay", "true");
        config.put("felix.fileinstall.log.level", "3"); // INFO level

        config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, 
            "org.slf4j;version=1.7.36,org.slf4j.helpers;version=1.7.36,org.slf4j.spi;version=1.7.36," +
            "com.sun.jna;version=5.12.1,com.sun.jna.win32;version=5.12.1," +
            "com.sun.jna.platform;version=5.12.1,com.sun.jna.platform.win32;version=5.12.1," +
            "com.sun.jna.ptr;version=5.12.1,com.sun.jna.structure;version=5.12.1"
        );

        FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
        framework = factory.newFramework(config);
        framework.init();
        
        BundleContext ctx = framework.getBundleContext();
        System.out.println("[Bootstrap] OSGi Framework initialized.");

        // === SMART DYNAMIC LOADING SEQUENCE ===
        
        // Level 0: Infrastructure
        String[] infra = {
            "sokybot-core/target/dependency/org.apache.felix.scr-2.2.6.jar",
            "sokybot-core/target/dependency/org.apache.felix.eventadmin-1.6.4.jar",
            "sokybot-core/target/dependency/org.apache.felix.configadmin-1.9.26.jar",
            "sokybot-core/target/dependency/org.apache.felix.fileinstall-3.7.4.jar"
        };
        
        // Level 1: Core API & Data
        String[] core = { "sokybot-api", "sokybot-security", "sokybot-pk2", "sokybot-game-loader", "sokybot-persistence" };
        
        // Level 2: Functional Logic & UI
        String[] apps = { "sokybot-packet-sniffer", "sokybot-engine", "sokybot-ui", "sokybot-builders" };

        loadBundles(ctx, infra, true); // Direct paths
        loadBundles(ctx, core, false); // Discoverable modules
        loadBundles(ctx, apps, false); // Discoverable modules

        framework.start();
        System.out.println("[Bootstrap] All modules started. UI should be appearing...");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("[Bootstrap] Shutting down framework...");
                framework.stop();
                framework.waitForStop(5000);
            } catch (Exception e) { e.printStackTrace(); }
        }));

        FrameworkEvent event = framework.waitForStop(0);
        System.out.println("[Bootstrap] Framework stopped: " + event.getType());
        System.exit(0);
    }

    private void loadBundles(BundleContext ctx, String[] targets, boolean isDirectPath) throws Exception {
        for (String target : targets) {
            String path = isDirectPath ? target : discoverBundle(target);
            if (path != null) {
                String fileUrl = "file:/" + Paths.get(path).toAbsolutePath().toString().replace("\\", "/");
                System.out.println("[Bootstrap] Loading " + target + " from " + path);
                Bundle b = ctx.installBundle(fileUrl);
                b.start();
            } else {
                System.err.println("[Bootstrap] WARNING: Could not discover bundle for " + target);
            }
        }
    }

    private String discoverBundle(String moduleName) {
        java.io.File targetDir = new java.io.File(moduleName, "target");
        if (targetDir.exists() && targetDir.isDirectory()) {
            java.io.File[] files = targetDir.listFiles((dir, name) -> name.endsWith(".jar") && !name.endsWith("-sources.jar") && !name.endsWith("-javadoc.jar"));
            if (files != null && files.length > 0) {
                // Return the first matching JAR (usually the main artifact)
                return files[0].getPath();
            }
        }
        return null;
    }
}
