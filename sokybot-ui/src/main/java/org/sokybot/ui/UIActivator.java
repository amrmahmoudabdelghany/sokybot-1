package org.sokybot.ui;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class UIActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("UIActivator: Bundle Started. Waiting for DS Components...");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("UIActivator: Bundle Stopped.");
    }
}
