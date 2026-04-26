package org.sokybot.behaviors.hunting;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.engine.api.behavior.BehaviorCycleSpec;
import org.sokybot.engine.api.behavior.IBehaviorCycleAssembler;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.workflow.ICycleDefinition;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.trade.coordination.api.SwarmRole;

@Component(service = IActuator.class, immediate = true)
public final class HuntingActuator implements IActuator {

    @Reference
    private volatile IBehaviorCycleAssembler behaviorCycleAssembler;

    @Override
    public String getName() {
        return "hunting";
    }

    @Override
    public void initialize(IActuatorContext context) {
        ISettingsRegistry registry = context.getService(ISettingsRegistry.class);
        if (registry != null && !registry.getRegisteredScopes().contains("hunting")) {
            registry.register("hunting", HuntingSettings.class, HuntingSettings::new);
        }
        if (registry == null) {
            return;
        }
        ISettingsProvider<HuntingSettings> provider = registry.getProvider(
                context.getGroupName(), context.getMachineName(), "hunting", HuntingSettings.class);
        if (provider == null) {
            return;
        }

        BehaviorCycleSpec<HuntingSettings> spec = BehaviorCycleSpec.builder(HuntingSettings.class)
                .cycleId(HuntingCycleKeys.CYCLE_NAME)
                .priority(236)
                .settingsSupplier(provider::get)
                .entryGuard(ctx -> {
                    HuntingSettings s = provider.get();
                    return s != null && s.getRole() != null && s.getRole() != SwarmRole.NONE;
                })
                .interruptible(true)
                .defaultDelayMs(350L)
                .build();
        ICycleDefinition cycle = behaviorCycleAssembler.assemble(spec);
        context.getWorkflowRegistry().registerCycle(cycle);
    }

    @Override
    public void shutdown(IActuatorContext context) {
        context.getWorkflowRegistry().unregisterCycle(HuntingCycleKeys.CYCLE_NAME);
    }
}
