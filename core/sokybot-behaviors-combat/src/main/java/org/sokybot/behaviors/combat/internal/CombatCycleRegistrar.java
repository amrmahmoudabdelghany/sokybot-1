package org.sokybot.behaviors.combat.internal;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.combat.api.ILeashAnchorStore;
import org.sokybot.town.api.IIntentArbiter;
import org.sokybot.town.api.IntentKind;
import org.sokybot.engine.api.behavior.BehaviorCycleSpec;
import org.sokybot.engine.api.behavior.IBehaviorCycleAssembler;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.workflow.ICycleDefinition;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gamemodel.model.ITrainer;
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

    @Reference
    private ILeashAnchorStore leashAnchorStore;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IIntentArbiter intentArbiter;

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

        stampLeashAnchor(context);

        BehaviorCycleSpec<CombatSettings> spec = BehaviorCycleSpec.builder(CombatSettings.class)
                .cycleId(CombatCycleKeys.CYCLE_NAME)
                .priority(250)
                .settingsSupplier(provider::get)
                .entryGuard(ctx -> combatEntryAllowed(ctx, provider))
                .interruptible(true)
                .defaultDelayMs(200L)
                .build();

        ICycleDefinition cycle = behaviorCycleAssembler.assemble(spec);
        context.getWorkflowRegistry().registerCycle(cycle);
        log.info("Registered {} for machine {}", CombatCycleKeys.CYCLE_NAME, context.getMachineId());
    }

    @Override
    public void shutdown(IActuatorContext context) {
        leashAnchorStore.clearAnchor(context.getMachineId());
        boolean ok = context.getWorkflowRegistry().unregisterCycle(CombatCycleKeys.CYCLE_NAME);
        log.info("Unregistered {} (success={})", CombatCycleKeys.CYCLE_NAME, ok);
    }

    private void stampLeashAnchor(IActuatorContext context) {
        String mid = context.getMachineId();
        ITrainer trainer = context.getGameModel().getTrainer();
        GamePosition gp = trainer != null ? trainer.getPosition() : null;
        if (gp != null) {
            float x = gp.getX();
            float y = gp.getY();
            float z = gp.getZ();
            leashAnchorStore.setAnchor(mid, x, y, z);
            Map<String, Object> sd = context.getSessionData();
            sd.put(CombatCycleKeys.KEY_LEASH_ANCHOR_X, Float.valueOf(x));
            sd.put(CombatCycleKeys.KEY_LEASH_ANCHOR_Y, Float.valueOf(y));
            sd.put(CombatCycleKeys.KEY_LEASH_ANCHOR_Z, Float.valueOf(z));
            log.debug("Combat: leash anchor set at ({},{},{}) for {}", Float.valueOf(x), Float.valueOf(y),
                    Float.valueOf(z), mid);
            return;
        }
        log.warn("Combat: trainer position unavailable; leash anchor not stamped for {}", mid);
    }

    private boolean combatEntryAllowed(IWorkflowContext ctx, ISettingsProvider<CombatSettings> provider) {
        try {
            IIntentArbiter arb = intentArbiter;
            if (arb != null) {
                IntentKind k = arb.decide(ctx.getMachineId());
                if (k == IntentKind.TOWN || k == IntentKind.DEATH) {
                    return false;
                }
            }
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
