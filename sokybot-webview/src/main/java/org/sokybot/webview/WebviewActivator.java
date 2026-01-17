package org.sokybot.webview;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class WebviewActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("Starting Webview Bundle (Activator)...");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("Stopping Webview Bundle (Activator)...");
    }
}
