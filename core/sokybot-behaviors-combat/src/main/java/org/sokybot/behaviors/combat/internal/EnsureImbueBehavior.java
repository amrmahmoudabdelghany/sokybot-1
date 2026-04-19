package org.sokybot.behaviors.combat.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.ActiveBuff;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.api.MonsterRef;
import org.sokybot.combat.projections.api.ICombatModel;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;

/**
 * Keeps imbue buffs refreshed during combat using {@link CombatSettings#getImbueSkillRotation()} and snapshot buff
 * projections.
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class EnsureImbueBehavior implements IBehavior<CombatSettings> {

    @Reference
    private ICombatModel combatModel;

    @Override
    public String id() {
        return CombatCycleKeys.BEHAVIOR_ENSURE_IMBUE;
    }

    @Override
    public int order() {
        return 35;
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
        if (settings == null) {
            return false;
        }
        List<Integer> imbueRot = settings.getImbueSkillRotation();
        if (imbueRot == null || imbueRot.isEmpty()) {
            return false;
        }
        Optional<ICombatSnapshot> snapOpt = combatModel.snapshot(context.getMachineId());
        if (!snapOpt.isPresent()) {
            return false;
        }
        ICombatSnapshot snap = snapOpt.get();
        Optional<Integer> tid = snap.getCurrentTargetEntityId();
        if (!tid.isPresent()) {
            return false;
        }
        if (!snap.getSelfEntityId().isPresent()) {
            return false;
        }
        if (!isTargetAlive(snap, tid.get().intValue())) {
            return false;
        }
        long now = System.currentTimeMillis();
        return pickImbueSkill(snap, settings, context.getPersistentData(), now).isPresent();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings settings) {
        Optional<ICombatSnapshot> snapOpt = combatModel.snapshot(context.getMachineId());
        if (!snapOpt.isPresent() || settings == null) {
            return BehaviorStatus.SKIPPED;
        }
        ICombatSnapshot snap = snapOpt.get();
        long now = System.currentTimeMillis();
        Map<String, Object> pd = context.getPersistentData();
        Optional<Integer> skill = pickImbueSkill(snap, settings, pd, now);
        if (!skill.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        int selfId = snap.getSelfEntityId().orElse(0);
        if (selfId <= 0) {
            return BehaviorStatus.SKIPPED;
        }
        CombatPackets.sendSkillCast(context, skill.get().intValue(), selfId);
        pd.put(CombatCycleKeys.KEY_IMBUE_LAST_SKILL_CAST_AT_MS, Long.valueOf(now));
        pd.put(CombatCycleKeys.KEY_IMBUE_ATTACKS_SINCE_LAST_CAST, Integer.valueOf(0));
        pd.put(CombatCycleKeys.KEY_COMBAT_PHASE, CombatCycleKeys.PHASE_ENGAGED);
        return BehaviorStatus.EXECUTED;
    }

    @Override
    public long postDelayMs() {
        return 250L;
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public int interruptionPriority() {
        return 45;
    }

    private Optional<Integer> pickImbueSkill(ICombatSnapshot snap, CombatSettings settings,
            Map<String, Object> persistentData, long now) {
        List<Integer> rot = settings.getImbueSkillRotation();
        long leadMs = settings.getImbueRefreshLeadMs();
        int maxAttacks = settings.getImbueMaxAttacksBetweenCasts();
        int attacksSince = 0;
        Object rawA = persistentData.get(CombatCycleKeys.KEY_IMBUE_ATTACKS_SINCE_LAST_CAST);
        if (rawA instanceof Number) {
            attacksSince = ((Number) rawA).intValue();
        }
        boolean byAttacks = maxAttacks > 0 && attacksSince >= maxAttacks;

        for (Integer skillRefObj : rot) {
            int skillRef = skillRefObj.intValue();
            Long readyAt = snap.getSkillCooldownReadyAtEpochMs().get(skillRef);
            if (readyAt != null && readyAt > now) {
                continue;
            }
            Optional<ActiveBuff> b = findBuff(snap, skillRef);
            boolean durationOk = b.isPresent() && imbueDurationOk(b.get(), now, leadMs);
            if (!durationOk || byAttacks) {
                return Optional.of(Integer.valueOf(skillRef));
            }
        }
        return Optional.empty();
    }

    private static Optional<ActiveBuff> findBuff(ICombatSnapshot snap, int skillRefId) {
        for (ActiveBuff b : snap.getActiveBuffs()) {
            if (b.getSkillRefId() == skillRefId) {
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }

    private static boolean imbueDurationOk(ActiveBuff b, long now, long leadMs) {
        long exp = b.getExpiresAtEpochMs();
        if (exp == Long.MAX_VALUE) {
            return true;
        }
        return exp > now + leadMs;
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
