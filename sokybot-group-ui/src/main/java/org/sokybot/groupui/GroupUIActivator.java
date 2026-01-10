package org.sokybot.groupui;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle activator for sokybot-group-ui.
 */
public class GroupUIActivator implements BundleActivator {
    
    private static final Logger log = LoggerFactory.getLogger(GroupUIActivator.class);

    @Override
    public void start(BundleContext context) throws Exception {
        log.info("sokybot-group-ui bundle started. Waiting for context lifecycle events...");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("sokybot-group-ui bundle stopped");
    }
}
