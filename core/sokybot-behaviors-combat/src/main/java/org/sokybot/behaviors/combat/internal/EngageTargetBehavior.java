package org.sokybot.behaviors.combat.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.combat.api.ICombatPolicy;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.api.ISkillRotation;
import org.sokybot.combat.api.MonsterRef;
import org.sokybot.combat.api.SkillAction;
import org.sokybot.combat.api.SkillActionKind;
import org.sokybot.combat.projections.api.ICombatModel;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;

@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class EngageTargetBehavior implements IBehavior<CombatSettings> {

    private static final String PD_SKILL_CURSOR = "combat.pd.skillRotationCursor";

    @Reference
    private ICombatModel combatModel;

    @Reference
    private ISkillRotation skillRotation;

    @Override
    public String id() {
        return CombatCycleKeys.BEHAVIOR_ENGAGE;
    }

    @Override
    public int order() {
        return 40;
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
        Optional<ICombatSnapshot> snap = combatModel.snapshot(context.getMachineId());
        if (!snap.isPresent() || settings == null) {
            return false;
        }
        Optional<Integer> tid = snap.get().getCurrentTargetEntityId();
        if (!tid.isPresent()) {
            return false;
        }
        if (!isTargetAlive(snap.get(), tid.get().intValue())) {
            return false;
        }
        ICombatPolicy pol = settings.toPolicy();
        Optional<SkillAction> action = resolveAction(snap.get(), settings, pol, context);
        return action.isPresent();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings settings) {
        Optional<ICombatSnapshot> snap = combatModel.snapshot(context.getMachineId());
        if (!snap.isPresent() || settings == null) {
            return BehaviorStatus.SKIPPED;
        }
        ICombatPolicy pol = settings.toPolicy();
        Optional<SkillAction> action = resolveAction(snap.get(), settings, pol, context);
        if (!action.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        SkillAction a = action.get();
        int target = a.getTargetEntityId();
        switch (a.getKind()) {
            case AUTO_ATTACK:
                incrementImbueAttackCounter(context);
                CombatPackets.sendCharActionAttack(context, target);
                break;
            case ATTACK_SKILL:
                incrementImbueAttackCounter(context);
                CombatPackets.sendSkillCast(context, a.getSkillRefId(), target);
                break;
            case BUFF:
            case IMBUE:
                CombatPackets.sendSkillCast(context, a.getSkillRefId(), target);
                break;
            default:
                return BehaviorStatus.SKIPPED;
        }
        context.getPersistentData().put(CombatCycleKeys.KEY_COMBAT_PHASE, CombatCycleKeys.PHASE_ENGAGED);
        context.getPersistentData().put(CombatCycleKeys.KEY_LAST_SKILL_EXECUTED_AT_MS,
                Long.valueOf(System.currentTimeMillis()));
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

    @Override
    public int interruptionPriority() {
        return 50;
    }

    private Optional<SkillAction> resolveAction(ICombatSnapshot snap, CombatSettings settings, ICombatPolicy policy,
            IWorkflowContext ctx) {
        List<Integer> rotation = settings.getAttackSkillRotation();
        if (rotation != null && !rotation.isEmpty()) {
            Optional<SkillAction> fromRotation = pickFromRotation(snap, rotation, ctx.getPersistentData());
            if (fromRotation.isPresent()) {
                return fromRotation;
            }
            // All rotation skills cooling: fall back to auto-attack delegate.
        }
        return skillRotation.nextAction(snap, policy);
    }

    private Optional<SkillAction> pickFromRotation(ICombatSnapshot snap, List<Integer> rotation,
            java.util.Map<String, Object> persistentData) {
        Optional<Integer> tid = snap.getCurrentTargetEntityId();
        if (!tid.isPresent()) {
            return Optional.empty();
        }
        int targetId = tid.get().intValue();
        long now = System.currentTimeMillis();
        int start = 0;
        Object raw = persistentData.get(PD_SKILL_CURSOR);
        if (raw instanceof Integer) {
            start = ((Integer) raw).intValue() % rotation.size();
        }
        for (int i = 0; i < rotation.size(); i++) {
            int idx = (start + i) % rotation.size();
            int skillRef = rotation.get(idx).intValue();
            Long readyAt = snap.getSkillCooldownReadyAtEpochMs().get(skillRef);
            if (readyAt != null && readyAt > now) {
                continue;
            }
            persistentData.put(PD_SKILL_CURSOR, Integer.valueOf((idx + 1) % rotation.size()));
            return Optional.of(new SkillAction(SkillActionKind.ATTACK_SKILL, skillRef, targetId, readyAt));
        }
        return Optional.empty();
    }

    private static void incrementImbueAttackCounter(IWorkflowContext context) {
        Map<String, Object> pd = context.getPersistentData();
        Object raw = pd.get(CombatCycleKeys.KEY_IMBUE_ATTACKS_SINCE_LAST_CAST);
        int v = raw instanceof Number ? ((Number) raw).intValue() : 0;
        pd.put(CombatCycleKeys.KEY_IMBUE_ATTACKS_SINCE_LAST_CAST, Integer.valueOf(v + 1));
    }

    private static boolean isTargetAlive(ICombatSnapshot snap, int targetId) {
        for (MonsterRef m : snap.getNearbyMonsters()) {
            if (m.getEntityId() == targetId) {
                return m.getHpPercentOrNegativeIfUnknown() != 0;
            }
        }
        return false;
    }
}
