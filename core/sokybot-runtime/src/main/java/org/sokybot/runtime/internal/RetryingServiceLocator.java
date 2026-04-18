package org.sokybot.runtime.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;

/**
 * OSGi service lookup with bounded retries (timing tolerance during startup).
 */
final class RetryingServiceLocator {

    private RetryingServiceLocator() {
    }

    static <T> T get(BundleContext bundleContext, Class<T> serviceClass, int maxAttempts, long delayMs, Logger log) {
        if (bundleContext == null) {
            return null;
        }

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                ServiceReference<T> ref = bundleContext.getServiceReference(serviceClass);
                if (ref != null) {
                    T service = bundleContext.getService(ref);
                    if (service != null) {
                        return service;
                    }
                }
            } catch (Exception e) {
                log.debug("Service {} not available (attempt {})", serviceClass.getName(), attempt + 1, e);
            }

            if (attempt < maxAttempts - 1) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.warn("Service {} not available after {} retries", serviceClass.getName(), maxAttempts);
        return null;
    }
}
