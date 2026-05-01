package org.sokybot.behaviors.navigation;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.engine.api.behavior.BehaviorCycleSpec;
import org.sokybot.engine.api.behavior.IBehaviorCycleAssembler;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.workflow.ICycleDefinition;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = IActuator.class, immediate = true)
public final class SynchronizedLureCycleRegistrar implements IActuator {

    private static final Logger log = LoggerFactory.getLogger(SynchronizedLureCycleRegistrar.class);

    private static final String SETTINGS_SCOPE = "swarmLure";

    @Reference
    private IBehaviorCycleAssembler behaviorCycleAssembler;

    @Override
    public String getName() {
        return "swarm-lure";
    }

    @Override
    public void initialize(IActuatorContext context) {
        ISettingsRegistry registry = context.getService(ISettingsRegistry.class);
        if (registry == null) {
            log.warn("Swarm lure: ISettingsRegistry unavailable; {} not registered", SwarmLureCycleKeys.CYCLE_NAME);
            return;
        }
        if (!registry.getRegisteredScopes().contains(SETTINGS_SCOPE)) {
            registry.register(SETTINGS_SCOPE, SwarmLureSettings.class, SwarmLureSettings::new);
        }
        ISettingsProvider<SwarmLureSettings> provider = registry.getProvider(
                context.getGroupName(),
                context.getMachineName(),
                SETTINGS_SCOPE,
                SwarmLureSettings.class);
        if (provider == null) {
            log.warn("Swarm lure: settings provider unavailable; {} not registered", SwarmLureCycleKeys.CYCLE_NAME);
            return;
        }

        BehaviorCycleSpec<SwarmLureSettings> spec = BehaviorCycleSpec.builder(SwarmLureSettings.class)
                .cycleId(SwarmLureCycleKeys.CYCLE_NAME)
                .priority(232)
                .settingsSupplier(provider::get)
                .entryGuard(ctx -> {
                    SwarmLureSettings s = provider.get();
                    return s != null && s.isSwarmLureCycleEnabled();
                })
                .interruptible(true)
                .defaultDelayMs(350L)
                .build();

        ICycleDefinition cycle = behaviorCycleAssembler.assemble(spec);
        context.getWorkflowRegistry().registerCycle(cycle);
        log.info("Registered {} for machine {}", SwarmLureCycleKeys.CYCLE_NAME, context.getMachineId());
    }

    @Override
    public void shutdown(IActuatorContext context) {
        boolean ok = context.getWorkflowRegistry().unregisterCycle(SwarmLureCycleKeys.CYCLE_NAME);
        log.info("Unregistered {} (success={})", SwarmLureCycleKeys.CYCLE_NAME, ok);
    }
}
