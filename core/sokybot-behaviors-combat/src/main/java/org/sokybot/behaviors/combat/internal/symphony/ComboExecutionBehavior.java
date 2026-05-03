package org.sokybot.behaviors.combat.internal.symphony;

import java.util.List;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.CombatPackets;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.behaviors.combat.internal.settings.symphony.ComboDefinition;
import org.sokybot.behaviors.combat.internal.settings.symphony.SymphonyComboSettings;
import org.sokybot.combat.api.CombatCycleKeys;
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
import org.sokybot.swarm.api.symphony.SwarmCombatEffectEvent;

/**
 * Epic #21 Phase 4: fires payload skill immediately after a confirmed symphony effect (tight window).
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE, property = "order=4")
public final class ComboExecutionBehavior implements IBehavior<CombatSettings> {

    private static final long EFFECT_EXECUTE_WINDOW_MS = 3000L;

    @Reference
    private ISettingsRegistry settingsRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISymphonyComboSignalCache symphonySignalCache;

    @Reference
    private ICombatModel combatModel;

    @Override
    public String id() {
        return "combat.symphony.comboExecution";
    }

    @Override
    public int order() {
        return 4;
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
        return resolveMatchingEffect(context).isPresent();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings settings) {
        Optional<ComboEffectMatch> match = resolveMatchingEffect(context);
        if (!match.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        ComboEffectMatch m = match.get();
        CombatPackets.sendSkillCast(context, m.combo.getPayloadSkillRefId(), m.selfTargetId);
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

    private Optional<ComboEffectMatch> resolveMatchingEffect(IWorkflowContext context) {
        ISymphonyComboSignalCache cache = symphonySignalCache;
        ISettingsRegistry registry = settingsRegistry;
        if (cache == null || registry == null || combatModel == null) {
            return Optional.empty();
        }
        if (!combatModel.snapshot(context.getMachineId()).isPresent()) {
            return Optional.empty();
        }

        SymphonyComboSettings sym = readSymphony(context, registry);
        if (sym == null || !sym.isSymphonyEnabled()) {
            return Optional.empty();
        }
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
            Optional<SwarmCombatEffectEvent> effectOpt =
                    cache.peekLatestEffect(cid, targetRefId);
            if (!effectOpt.isPresent()) {
                continue;
            }
            SwarmCombatEffectEvent effect = effectOpt.get();
            if (now - effect.getEffectEpochMs() >= EFFECT_EXECUTE_WINDOW_MS) {
                continue;
            }
            return Optional.of(new ComboEffectMatch(combo, selfTargetId));
        }
        return Optional.empty();
    }

    private static Optional<Integer> resolveTargetRefId(IGameModel gameModel, int entityId) {
        try {
            Optional<ISpawn> spawnOpt = gameModel.find(entityId);
            return spawnOpt.map(ISpawn::getRefId);
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

    private static final class ComboEffectMatch {
        final ComboDefinition combo;
        final int selfTargetId;

        ComboEffectMatch(ComboDefinition combo, int selfTargetId) {
            this.combo = combo;
            this.selfTargetId = selfTargetId;
        }
    }
}
