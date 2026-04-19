package org.sokybot.behaviors.town.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.behaviors.town.internal.settings.TownSettings;
import org.sokybot.engine.api.behavior.BehaviorCycleSpec;
import org.sokybot.engine.api.behavior.IBehaviorCycleAssembler;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.workflow.ICycleDefinition;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.town.api.IIntentArbiter;
import org.sokybot.town.api.IntentKind;
import org.sokybot.town.api.TownCycleKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = IActuator.class, immediate = true)
public final class TownCycleRegistrar implements IActuator {

    private static final Logger log = LoggerFactory.getLogger(TownCycleRegistrar.class);

    @Reference
    private IBehaviorCycleAssembler behaviorCycleAssembler;

    @Reference
    private IIntentArbiter intentArbiter;

    @Override
    public String getName() {
        return "town";
    }

    @Override
    public void initialize(IActuatorContext context) {
        ISettingsRegistry registry = context.getService(ISettingsRegistry.class);
        if (registry == null) {
            log.warn("Town: ISettingsRegistry unavailable; town-cycle not registered");
            return;
        }
        if (!registry.getRegisteredScopes().contains("town")) {
            registry.register("town", TownSettings.class, TownSettings::new);
        }
        ISettingsProvider<TownSettings> provider = registry.getProvider(
                context.getGroupName(),
                context.getMachineName(),
                "town",
                TownSettings.class);
        if (provider == null) {
            log.warn("Town: settings provider unavailable; town-cycle not registered");
            return;
        }

        BehaviorCycleSpec<TownSettings> spec = BehaviorCycleSpec.builder(TownSettings.class)
                .cycleId(TownCycleKeys.CYCLE_NAME)
                .priority(200)
                .settingsSupplier(provider::get)
                .entryGuard(ctx -> townEntryAllowed(ctx))
                .interruptible(true)
                .defaultDelayMs(250L)
                .build();

        ICycleDefinition cycle = behaviorCycleAssembler.assemble(spec);
        context.getWorkflowRegistry().registerCycle(cycle);
        log.info("Registered {} for machine {}", TownCycleKeys.CYCLE_NAME, context.getMachineId());
    }

    @Override
    public void shutdown(IActuatorContext context) {
        boolean ok = context.getWorkflowRegistry().unregisterCycle(TownCycleKeys.CYCLE_NAME);
        log.info("Unregistered {} (success={})", TownCycleKeys.CYCLE_NAME, ok);
    }

    private boolean townEntryAllowed(IWorkflowContext ctx) {
        IntentKind k = intentArbiter.decide(ctx.getMachineId());
        return k == IntentKind.TOWN || k == IntentKind.DEATH;
    }
}
