package org.sokybot.actuator.login;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi bundle activator for login actuator.
 */
public class LoginActuatorActivator implements BundleActivator {
    
    private static final Logger log = LoggerFactory.getLogger(LoginActuatorActivator.class);
    
    @Override
    public void start(BundleContext context) throws Exception {
        log.info("Login actuator bundle started");
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("Login actuator bundle stopped");
    }
}
