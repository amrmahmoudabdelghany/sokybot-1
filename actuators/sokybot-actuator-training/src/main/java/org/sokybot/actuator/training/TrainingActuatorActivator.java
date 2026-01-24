package org.sokybot.actuator.training;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi bundle activator for training actuator.
 */
public class TrainingActuatorActivator implements BundleActivator {
    
    private static final Logger log = LoggerFactory.getLogger(TrainingActuatorActivator.class);
    
    @Override
    public void start(BundleContext context) throws Exception {
        log.info("Training actuator bundle started");
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("Training actuator bundle stopped");
    }
}
