package org.sokybot.engine;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class EngineActivator implements BundleActivator {

    private static BundleContext bundleContext;
    private ConfigurableApplicationContext springContext;

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("Sokybot Engine Starting...");
        bundleContext = context;
        
        // Use the current thread's classloader to avoid issues with Spring/OSGi
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            this.springContext = new SpringApplicationBuilder(EngineConfig.class)
                .web(WebApplicationType.NONE)
                .run();
            System.out.println("Sokybot Engine: Spring context started.");
        } finally {
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("Sokybot Engine Stopping...");
        if (this.springContext != null) {
            this.springContext.close();
        }
        bundleContext = null;
    }

    public static BundleContext getBundleContext() {
        return bundleContext;
    }
}
