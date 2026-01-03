package org.sokybot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Slf4j
@ComponentScan({ "org.sokybot.app" })
@Configuration
public class Driver {

    public static void main(String[] args) {
        // Disable Hardware Acceleration for VMware compatibility
        System.setProperty("sun.java2d.noddraw", "true");
        System.setProperty("sun.java2d.d3d", "false");
        System.setProperty("sun.java2d.opengl", "false");
        System.setProperty("sun.java2d.pmoffscreen", "false");
        
        // Verbose AWT logging
        // System.setProperty("sun.awt.debug.log", "true");
        // System.setProperty("sun.awt.nativedebug", "true");

        System.out.println("DRIVER: Starting (VMware compatibility mode)...");
        
        try {
            // Manual OSGi Bootstrap
            System.out.println("DRIVER: Manually Initializing OSGi...");
            java.util.Map<String, String> config = new java.util.HashMap<>();
            config.put("org.osgi.framework.storage.clean", "onFirstInit");
            config.put("org.osgi.framework.storage", "felix-cache");
            config.put("org.osgi.framework.system.packages.extra", 
                "org.slf4j;version=1.7.36,org.slf4j.helpers;version=1.7.36,org.slf4j.spi;version=1.7.36," +
                "org.springframework.context;version=5.3.27,org.springframework.context.annotation;version=5.3.27," +
                "org.springframework.beans.factory.annotation;version=5.3.27,org.springframework.stereotype;version=5.3.27," +
                "org.springframework.util;version=5.3.27,org.springframework.core.io;version=5.3.27," + 
                "javax.annotation;version=1.3.2," +
                "com.sun.jna;version=5.12.1,com.sun.jna.win32;version=5.12.1," +
                "com.sun.jna.platform;version=5.12.1,com.sun.jna.platform.win32;version=5.12.1," +
                "com.sun.jna.ptr;version=5.12.1,com.sun.jna.structure;version=5.12.1,com.sun.jna.win32.core;version=5.12.1"
            );

            org.osgi.framework.launch.FrameworkFactory factory = java.util.ServiceLoader.load(org.osgi.framework.launch.FrameworkFactory.class).iterator().next();
            org.osgi.framework.launch.Framework framework = factory.newFramework(config);
            framework.init();
            
            org.osgi.framework.BundleContext ctx = framework.getBundleContext();
            System.out.println("DRIVER: OSGi Framework Initialized. Context: " + ctx);

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
                    System.out.println("DRIVER: Installing bundle: " + fileUrl);
                    org.osgi.framework.Bundle b = ctx.installBundle(fileUrl);
                    b.start();
                    System.out.println("DRIVER: Started bundle: " + b.getSymbolicName());
                } catch (Exception e) {
                    System.err.println("DRIVER: Failed for module: " + modulePath);
                    e.printStackTrace();
                }
            }
            framework.start();
            System.out.println("DRIVER: OSGi Framework Started.");

        } catch (Throwable t) {
            System.err.println("DRIVER: CRITICAL ERROR during manual OSGi launch:");
            t.printStackTrace();
        }
     
        // Keep the JVM alive
        System.out.println("DRIVER: Main thread entering loop (LOCKED)...");
        try {
            while (true) {
                Thread.sleep(2000); // Shorter sleep for better resolution
                System.out.println("DRIVER: TICK - " + new java.util.Date());
            }
        } catch (InterruptedException e) {
             System.err.println("DRIVER: Main thread interrupted!");
        } finally {
            System.out.println("DRIVER: Main thread finally exiting.");
        }
    }
}
