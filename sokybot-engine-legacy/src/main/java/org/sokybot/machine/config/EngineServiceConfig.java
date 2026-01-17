package org.sokybot.machine.config;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.sokybot.game.asset.IMediaAssetProvider;
import org.sokybot.game.navigation.IRuteFinder;
import org.sokybot.game.navigation.IRuteFinderFactory;
import org.sokybot.persistence.service.IGamePersistenceFactory;
import org.sokybot.persistence.service.IGameDataLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class EngineServiceConfig {


    private BundleContext bundleContext;

    @org.springframework.beans.factory.annotation.Value("${gamePath}")
    private String gamePath;

    @Bean
    public IGameDataLookup gameDataLookup() {
        IGamePersistenceFactory factory = getService(IGamePersistenceFactory.class)
                .orElseThrow(() -> new IllegalStateException("IGamePersistenceFactory service not found"));
        return factory.registerGame(this.gamePath);
    }

    @Bean
    public IMediaAssetProvider mediaAssetProvider() {
        return getService(IMediaAssetProvider.class)
                .orElseThrow(() -> new IllegalStateException("IMediaAssetProvider service not found"));
    }

    @Bean
    public IRuteFinder ruteFinder(IGameDataLookup gameDataLookup) {
        IRuteFinderFactory factory = getService(IRuteFinderFactory.class)
                .orElseThrow(() -> new IllegalStateException("IRuteFinderFactory service not found"));
        return factory.createRuteFinder(gameDataLookup);
    }

    private <T> Optional<T> getService(Class<T> clazz) {
        if (bundleContext == null) {
            return Optional.empty();
        }
        ServiceReference<T> ref = bundleContext.getServiceReference(clazz);
        if (ref != null) {
            return Optional.ofNullable(bundleContext.getService(ref));
        }
        return Optional.empty();
    }
}
