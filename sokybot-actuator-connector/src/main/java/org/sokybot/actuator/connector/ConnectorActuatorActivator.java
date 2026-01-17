package org.sokybot.actuator.connector;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi bundle activator for connector actuator.
 */
public class ConnectorActuatorActivator implements BundleActivator {
    
    private static final Logger log = LoggerFactory.getLogger(ConnectorActuatorActivator.class);
    
    @Override
    public void start(BundleContext context) throws Exception {
        log.info("Connector actuator bundle started");
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("Connector actuator bundle stopped");
    }
}
