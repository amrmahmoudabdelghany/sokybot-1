package org.sokybot.runtime.internal.fleet;

import org.osgi.service.component.annotations.Component;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.settings.fleet.FleetMachineSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers the {@code fleet} settings scope for {@link FleetMachineSettings} on each machine engine (Epic #16).
 */
@Component(service = IActuator.class, immediate = true)
public final class FleetSettingsActuator implements IActuator {

    private static final Logger log = LoggerFactory.getLogger(FleetSettingsActuator.class);

    @Override
    public String getName() {
        return "fleet-settings";
    }

    @Override
    public void initialize(IActuatorContext context) {
        ISettingsRegistry registry = context.getService(ISettingsRegistry.class);
        if (registry == null) {
            log.warn("Fleet settings: ISettingsRegistry unavailable for machine {}", context.getMachineId());
            return;
        }
        if (!registry.getRegisteredScopes().contains(FleetMachineSettings.SCOPE_NAME)) {
            registry.register(FleetMachineSettings.SCOPE_NAME, FleetMachineSettings.class, FleetMachineSettings::new);
        }
        log.debug(
                "Fleet settings scope '{}' ensured for machine {}",
                FleetMachineSettings.SCOPE_NAME,
                context.getMachineId());
    }

    @Override
    public void shutdown(IActuatorContext context) {
        // Scope registration is JVM-global; no unregister API on ISettingsRegistry.
    }
}
