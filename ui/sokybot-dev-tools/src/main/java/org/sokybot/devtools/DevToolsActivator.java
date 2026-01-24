package org.sokybot.devtools;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class DevToolsActivator {

    private static final Logger logger = LoggerFactory.getLogger(DevToolsActivator.class);

    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext context) {
        this.bundleContext = context;
        logger.info("DevTools bundle activated");
    }

    @Deactivate
    public void deactivate() {
        logger.info("DevTools bundle deactivated");
    }
}
