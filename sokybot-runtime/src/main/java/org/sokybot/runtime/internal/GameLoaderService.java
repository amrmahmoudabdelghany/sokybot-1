package org.sokybot.runtime.internal;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.sokybot.loader.IGameLoader;
import org.sokybot.loader.IGameLoaderService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for discovering and managing IGameLoader OSGi services.
 * 
 * Uses BundleContext directly to find and track IGameLoader services
 * instead of going through IPluginService abstraction.
 */
@Component(service = IGameLoaderService.class)
public class GameLoaderService implements IGameLoaderService, ServiceListener {
	
	private static final Logger log = LoggerFactory.getLogger(GameLoaderService.class);

    private Map<String, IGameLoader> gameLoaders = new ConcurrentHashMap<>();
    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        init();
    }
    
    void init() {
        if (bundleContext == null) {
            log.warn("BundleContext not available - GameLoaderService will not work");
            return;
        }

        try {
            // Add service listener for dynamic service discovery
            String filter = "(objectClass=" + IGameLoader.class.getName() + ")";
            bundleContext.addServiceListener(this, filter);
            log.info("Listening for {} services", IGameLoader.class.getName());

            // Find existing services
            ServiceReference<?>[] references = bundleContext.getAllServiceReferences(
                    IGameLoader.class.getName(), null);
            if (references != null) {
                for (ServiceReference<?> ref : references) {
                    IGameLoader loader = (IGameLoader) bundleContext.getService(ref);
                    if (loader != null) {
                        gameLoaders.put(loader.getName(), loader);
                        log.info("Found game loader: {}", loader.getName());
                    }
                }
                log.info("Found {} game loader(s) on startup", references.length);
            } else {
                log.info("No game loaders found on startup");
            }
        } catch (InvalidSyntaxException e) {
            log.error("Invalid service filter syntax", e);
        }
    }

    @Deactivate
    void destroy() {
        if (bundleContext != null) {
            bundleContext.removeServiceListener(this);
        }
    }

    @Override
    public Optional<IGameLoader> findGameLoader(String name) {
        return Optional.ofNullable(this.gameLoaders.get(name));
    }

    @Override
    public Set<String> listAvailables() {
        return this.gameLoaders.keySet();
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        if (bundleContext == null) {
            return;
        }

        ServiceReference<?> ref = event.getServiceReference();
        if (ref == null) {
            return;
        }

        Object service = bundleContext.getService(ref);
        if (!(service instanceof IGameLoader)) {
            return;
        }

        IGameLoader loader = (IGameLoader) service;
        int eventType = event.getType();

        if (eventType == ServiceEvent.REGISTERED) {
            this.gameLoaders.put(loader.getName(), loader);
            log.info("Game loader {} has been registered", loader.getName());
        } else if (eventType == ServiceEvent.UNREGISTERING) {
            this.gameLoaders.remove(loader.getName());
            log.info("Game loader {} has been removed", loader.getName());
            bundleContext.ungetService(ref);
        }
    }
}
