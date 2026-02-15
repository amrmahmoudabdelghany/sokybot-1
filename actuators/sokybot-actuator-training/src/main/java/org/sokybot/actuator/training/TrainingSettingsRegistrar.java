package org.sokybot.actuator.training;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.settings.api.ISettingsRegistry;

/**
 * Global registrar for training settings scope.
 * Ensures the 'training' scope is registered immediately when the bundle
 * starts.
 */
@Component(immediate = true)
public class TrainingSettingsRegistrar {

    private static final Logger log = LoggerFactory.getLogger(TrainingSettingsRegistrar.class);

    private volatile ISettingsRegistry settingsRegistry;

    @Reference
    public void setSettingsRegistry(ISettingsRegistry settingsRegistry) {
        this.settingsRegistry = settingsRegistry;
    }

    @Activate
    public void activate() {
        log.info("Registering training settings scope globally");
        settingsRegistry.register("training", TrainingSettings.class, TrainingSettings::new);
    }
}
