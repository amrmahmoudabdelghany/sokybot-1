package org.sokybot.engine;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class EngineActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("Sokybot Engine Starting...");
        // TODO: Initialize Logic, Spring Context, and register IMachineService
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("Sokybot Engine Stopping...");
    }
}
