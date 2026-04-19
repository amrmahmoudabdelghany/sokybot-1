package org.sokybot.behaviors.combat.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.engine.api.behavior.BehaviorCycleSpec;
import org.sokybot.engine.api.behavior.IBehaviorCycleAssembler;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.workflow.ICycleDefinition;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers {@link CombatCycleKeys#CYCLE_NAME} using {@link IBehaviorCycleAssembler}.
 */
@Component(service = IActuator.class, immediate = true)
public final class CombatCycleRegistrar implements IActuator {

    private static final Logger log = LoggerFactory.getLogger(CombatCycleRegistrar.class);

    @Reference
    private IBehaviorCycleAssembler behaviorCycleAssembler;

    @Override
    public String getName() {
        return "combat";
    }

    @Override
    public void initialize(IActuatorContext context) {
        ISettingsRegistry registry = context.getService(ISettingsRegistry.class);
        if (registry == null) {
            log.warn("Combat: ISettingsRegistry unavailable; combat-cycle not registered");
            return;
        }
        if (!registry.getRegisteredScopes().contains("combat")) {
            registry.register("combat", CombatSettings.class, CombatSettings::new);
        }
        ISettingsProvider<CombatSettings> provider = registry.getProvider(
                context.getGroupName(),
                context.getMachineName(),
                "combat",
                CombatSettings.class);
        if (provider == null) {
            log.warn("Combat: settings provider unavailable; combat-cycle not registered");
            return;
        }

        BehaviorCycleSpec<CombatSettings> spec = BehaviorCycleSpec.builder(CombatSettings.class)
                .cycleId(CombatCycleKeys.CYCLE_NAME)
                .priority(250)
                .settingsSupplier(provider::get)
                .entryGuard(ctx -> loggedInWithAutoAttack(ctx, provider))
                .interruptible(true)
                .defaultDelayMs(200L)
                .build();

        ICycleDefinition cycle = behaviorCycleAssembler.assemble(spec);
        context.getWorkflowRegistry().registerCycle(cycle);
        log.info("Registered {} for machine {}", CombatCycleKeys.CYCLE_NAME, context.getMachineId());
    }

    @Override
    public void shutdown(IActuatorContext context) {
        boolean ok = context.getWorkflowRegistry().unregisterCycle(CombatCycleKeys.CYCLE_NAME);
        log.info("Unregistered {} (success={})", CombatCycleKeys.CYCLE_NAME, ok);
    }

    private static boolean loggedInWithAutoAttack(IWorkflowContext ctx, ISettingsProvider<CombatSettings> provider) {
        try {
            CombatSettings s = provider.get();
            if (s == null || !s.isAutoAttack()) {
                return false;
            }
            return ctx.getGameModel().getTrainer().getUniqueId() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
