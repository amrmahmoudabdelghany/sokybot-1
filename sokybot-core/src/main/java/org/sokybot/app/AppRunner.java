package org.sokybot.app;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.felix.framework.Felix;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppRunner implements ApplicationRunner {

    private Framework framework;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("Initializing OSGi Framework...");

        Map<String, String> config = new HashMap<>();
        config.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        config.put(Constants.FRAMEWORK_STORAGE, "felix-cache");
        
        // Export packages from System Bundle (Host) to Bundles
         config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, 
             "org.slf4j;version=1.7.36,org.slf4j.helpers;version=1.7.36,org.slf4j.spi;version=1.7.36," +
             "org.springframework.context;version=5.3.27,org.springframework.context.annotation;version=5.3.27," +
             "org.springframework.beans.factory.annotation;version=5.3.27,org.springframework.stereotype;version=5.3.27," +
             "org.springframework.util;version=5.3.27,org.springframework.core.io;version=5.3.27," + 
             "javax.annotation;version=1.3.2," +
             "com.sun.jna;version=5.12.1,com.sun.jna.win32;version=5.12.1," +
             "com.sun.jna.platform;version=5.12.1,com.sun.jna.platform.win32;version=5.12.1," +
             "com.sun.jna.ptr;version=5.12.1,com.sun.jna.structure;version=5.12.1,com.sun.jna.win32.core;version=5.12.1"
         );

        // Add Shutdown Hook to see WHO is killing the JVM
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("CRITICAL: SHUTDOWN HOOK TRIGGERED!");
            try {
                for (Thread t : Thread.getAllStackTraces().keySet()) {
                    if (t.isAlive()) {
                        System.err.println("SHUTDOWN HOOK: Alive Thread: " + t.getName() + " (Daemon: " + t.isDaemon() + ")");
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }));

        FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
        framework = factory.newFramework(config);
        
        framework.init();
        
        BundleContext ctx = framework.getBundleContext();
        System.out.println("OSGi Framework Started. Bundle Context: " + ctx);
        
        // Define module paths - when using -Pprod profile, bundles are in system directory
        String[] modules = {
             "../Sokybot-1.0-SNAPSHOT/system/sokybot-security-1.0-SNAPSHOT.jar",
             "../Sokybot-1.0-SNAPSHOT/system/sokybot-pk2-1.0-SNAPSHOT.jar",
            "../Sokybot-1.0-SNAPSHOT/system/sokybot-api-1.0-SNAPSHOT.jar",
            "../sokybot-engine/target/sokybot-engine-1.0-SNAPSHOT.jar",
            "../Sokybot-1.0-SNAPSHOT/system/sokybot-ui-1.0-SNAPSHOT.jar",
            "../sokybot-map-debug/target/sokybot-map-debug-1.0-SNAPSHOT.jar"
        };
        
        for (String modulePath : modules) {
            try {
                String fileUrl = "file:/" + java.nio.file.Paths.get(modulePath).toAbsolutePath().toString().replace("\\", "/");
                 System.out.println("Installing bundle: " + fileUrl);
                Bundle b = ctx.installBundle(fileUrl);
                b.start();
                System.out.println("Started bundle: " + b.getSymbolicName());
            } catch (Exception e) {
                System.err.println("Failed to install/start module: " + modulePath);
                e.printStackTrace();
            }
        }
        
        framework.start();
        
        // Keep the application running until the framework is stopped
        // This prevents the JVM from exiting after bundle initialization
        System.out.println("Application started successfully. Waiting for shutdown...");
        
        // Add listener to see when/why framework stops
        framework.getBundleContext().addFrameworkListener(event -> {
            System.out.println("Framework event: " + event.getType() + " - " + event);
            if (event.getThrowable() != null) {
                System.err.println("Framework event has error:");
                event.getThrowable().printStackTrace();
            }
        });
        
        System.out.println("Framework state before wait: " + framework.getState());
        
        // AGGRESSIVE BLOCKING: Don't just rely on non-daemon thread, block the main thread too.
        System.out.println("BLOCKING MAIN THREAD INDEFINITELY...");
        try {
            while (framework.getState() == Bundle.ACTIVE) {
                Thread.sleep(10000);
                System.out.println("MAIN THREAD TICK: Framework is still ACTIVE (" + new java.util.Date() + ")");
            }
        } catch (InterruptedException e) {
            System.err.println("Main runner thread interrupted.");
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Main runner thread exiting. Framework state: " + framework.getState());
    }
}
