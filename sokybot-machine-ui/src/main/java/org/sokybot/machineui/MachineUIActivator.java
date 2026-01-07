package org.sokybot.machineui;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle activator for sokybot-machine-ui.
 * 
 * This bundle listens to context lifecycle events via OSGi Event Admin
 * and creates/destroys machine UI components accordingly.
 */
public class MachineUIActivator implements BundleActivator {
    
    private static final Logger log = LoggerFactory.getLogger(MachineUIActivator.class);

    @Override
    public void start(BundleContext context) throws Exception {
        log.info("sokybot-machine-ui bundle started. Waiting for context lifecycle events...");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("sokybot-machine-ui bundle stopped");
    }
}
