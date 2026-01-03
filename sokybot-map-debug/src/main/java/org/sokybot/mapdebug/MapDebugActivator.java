package org.sokybot.mapdebug;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class MapDebugActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("Map Debug Plugin Starting...");
        // TODO: Register IGameUIExtension with ViewerPanel
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}
