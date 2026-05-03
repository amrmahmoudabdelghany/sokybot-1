package org.sokybot.behaviors.combat.internal.symphony;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.behaviors.combat.internal.settings.symphony.ComboDefinition;
import org.sokybot.behaviors.combat.internal.settings.symphony.SymphonyComboSettings;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.projections.api.ICombatModel;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.model.ISpawn;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.swarm.api.symphony.ISymphonyComboSignalCache;
import org.sokybot.swarm.api.symphony.SwarmCombatIntentEvent;

/**
 * Epic #21 Phase 3: consumes combat-cycle ticks while waiting for symphony payload timing (payload skill must be ready).
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE, property = "order=5")
public final class HoldForComboBehavior implements IBehavior<CombatSettings> {

    @Reference
    private ISettingsRegistry settingsRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISymphonyComboSignalCache symphonySignalCache;

    @Reference
    private ICombatModel combatModel;

    @Override
    public String id() {
        return "combat.symphony.holdForCombo";
    }

    @Override
    public int order() {
        return 5;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return CombatCycleKeys.CYCLE_NAME.equals(cycleId);
    }

    @Override
    public Class<CombatSettings> settingsType() {
        return CombatSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, CombatSettings settings) {
        return resolveHold(context).isPresent();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings settings) {
        if (!resolveHold(context).isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        return BehaviorStatus.EXECUTED;
    }

    @Override
    public long postDelayMs() {
        return 200L;
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    private Optional<String> resolveHold(IWorkflowContext context) {
        ISymphonyComboSignalCache cache = symphonySignalCache;
        ISettingsRegistry registry = settingsRegistry;
        ICombatModel model = combatModel;
        if (cache == null || registry == null || model == null) {
            return Optional.empty();
        }
        SymphonyComboSettings sym = readSymphony(context, registry);
        if (sym == null || !sym.isSymphonyEnabled()) {
            return Optional.empty();
        }
        Optional<ICombatSnapshot> snapOpt = snapshot(model, context.getMachineId());
        if (!snapOpt.isPresent()) {
            return Optional.empty();
        }
        ICombatSnapshot snap = snapOpt.get();
        IGameModel gameModel = context.getGameModel();
        if (gameModel == null) {
            return Optional.empty();
        }
        ITrainer trainer = gameModel.getTrainer();
        if (trainer == null) {
            return Optional.empty();
        }
        int selfTargetId = trainer.getTargetId();
        if (selfTargetId <= 0) {
            return Optional.empty();
        }
        Optional<Integer> targetRefOpt = resolveTargetRefId(gameModel, selfTargetId);
        if (!targetRefOpt.isPresent()) {
            return Optional.empty();
        }
        int targetRefId = targetRefOpt.get().intValue();

        List<ComboDefinition> combos = sym.getCombos();
        if (combos == null || combos.isEmpty()) {
            return Optional.empty();
        }

        long now = System.currentTimeMillis();
        for (ComboDefinition combo : combos) {
            if (combo == null || combo.getComboId() == null || combo.getComboId().trim().isEmpty()) {
                continue;
            }
            String cid = combo.getComboId().trim();
            Optional<SwarmCombatIntentEvent> intentOpt = cache.peekLatestIntent(cid, targetRefId);
            if (!intentOpt.isPresent()) {
                continue;
            }
            SwarmCombatIntentEvent intent = intentOpt.get();
            if (now - intent.getIntentEpochMs() >= combo.getMaxWaitTimeMs()) {
                continue;
            }
            int payloadRef = combo.getPayloadSkillRefId();
            if (!isPayloadSkillReadyToCast(snap, payloadRef, now)) {
                continue;
            }
            return Optional.of(cid);
        }
        return Optional.empty();
    }

    private static boolean isPayloadSkillReadyToCast(ICombatSnapshot snap, int payloadSkillRefId, long now) {
        if (snap.isSkillCastInFlight()) {
            return false;
        }
        Map<Integer, Long> readyBySkill = snap.getSkillCooldownReadyAtEpochMs();
        Long readyAt = readyBySkill.get(Integer.valueOf(payloadSkillRefId));
        return readyAt == null || readyAt.longValue() <= now;
    }

    private static Optional<Integer> resolveTargetRefId(IGameModel gameModel, int entityId) {
        try {
            Optional<ISpawn> spawnOpt = gameModel.find(entityId);
            return spawnOpt.map(ISpawn::getRefId);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static Optional<ICombatSnapshot> snapshot(ICombatModel model, String machineId) {
        if (model == null || machineId == null) {
            return Optional.empty();
        }
        try {
            return model.snapshot(machineId);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static SymphonyComboSettings readSymphony(IWorkflowContext ctx, ISettingsRegistry registry) {
        try {
            ISettingsProvider<SymphonyComboSettings> p = registry.getProvider(
                    ctx.getGroupName(),
                    ctx.getMachineName(),
                    "symphony",
                    SymphonyComboSettings.class);
            return p != null ? p.get() : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
