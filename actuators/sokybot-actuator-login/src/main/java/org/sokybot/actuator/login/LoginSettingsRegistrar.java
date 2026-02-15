package org.sokybot.actuator.login;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.settings.api.ISettingsRegistry;

/**
 * Global registrar for login settings scope.
 * Ensures the 'login' scope is registered immediately when the bundle starts.
 */
@Component(immediate = true)
public class LoginSettingsRegistrar {

    private static final Logger log = LoggerFactory.getLogger(LoginSettingsRegistrar.class);

    private volatile ISettingsRegistry settingsRegistry;

    @Reference
    public void setSettingsRegistry(ISettingsRegistry settingsRegistry) {
        this.settingsRegistry = settingsRegistry;
    }

    @Activate
    public void activate() {
        log.info("Registering login settings scope globally");
        settingsRegistry.register("login", LoginSettings.class, LoginSettings::new);
    }
}
