package org.sokybot.behaviors.social.settings;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.behaviors.social.webhook.WebhookSettings;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.settings.api.ISettingsRegistry;

@Component(service = IActuator.class, immediate = true)
public class SocialActuator implements IActuator {

    private static final Logger log = LoggerFactory.getLogger(SocialActuator.class);

    @Override
    public String getName() {
        return "social";
    }

    @Override
    public void initialize(IActuatorContext context) {
        ISettingsRegistry registry = context.getService(ISettingsRegistry.class);
        if (registry != null) {
            if (!registry.getRegisteredScopes().contains("social")) {
                registry.register("social", SocialSettings.class, SocialSettings::new);
                log.info("Registered 'social' settings scope");
            }
            if (!registry.getRegisteredScopes().contains("webhooks")) {
                registry.register("webhooks", WebhookSettings.class, WebhookSettings::new);
                log.info("Registered 'webhooks' settings scope");
            }
        } else {
            log.warn("ISettingsRegistry not available; cannot register social settings scope");
        }
    }

    @Override
    public void shutdown(IActuatorContext context) {
        // Nothing specific to clean up for the social actuator
    }
}
