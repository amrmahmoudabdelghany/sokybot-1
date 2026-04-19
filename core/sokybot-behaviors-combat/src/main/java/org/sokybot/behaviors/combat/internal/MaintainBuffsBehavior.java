package org.sokybot.behaviors.combat.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.ActiveBuff;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.api.IWeaponSwapCoordinator;
import org.sokybot.combat.api.WeaponLoadout;
import org.sokybot.combat.projections.api.ICombatModel;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Out-of-combat buff maintenance using optional weapon swaps and {@link CombatSettings#getBuffCasterSkillRotation()}.
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class MaintainBuffsBehavior implements IBehavior<CombatSettings> {

    private static final Logger log = LoggerFactory.getLogger(MaintainBuffsBehavior.class);

    /** Integer: {@code 1} = casting phase, {@code 2} = swap back to main damage. */
    private static final String PD_MAINTAIN_STEP = "combat.maintainBuffs.step";
    private static final String PD_MAINTAIN_IDX = "combat.maintainBuffs.skillIndex";

    private static final int STEP_CAST = 1;
    private static final int STEP_SWAP_BACK = 2;

    @Reference
    private ICombatModel combatModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IWeaponSwapCoordinator swapCoordinator;

    @Override
    public String id() {
        return CombatCycleKeys.BEHAVIOR_MAINTAIN_BUFFS;
    }

    @Override
    public int order() {
        return 25;
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
        if (swapCoordinator == null || settings == null) {
            return false;
        }
        List<Integer> rot = settings.getBuffCasterSkillRotation();
        if (rot == null || rot.isEmpty()) {
            return false;
        }
        Optional<ICombatSnapshot> snapOpt = combatModel.snapshot(context.getMachineId());
        if (!snapOpt.isPresent()) {
            return false;
        }
        Map<String, Object> pd = context.getPersistentData();
        Object stepRaw = pd.get(PD_MAINTAIN_STEP);
        if (stepRaw instanceof Integer) {
            return true;
        }
        return idlePreconditions(context, snapOpt.get(), pd);
    }

    private static boolean idlePreconditions(IWorkflowContext context, ICombatSnapshot snap, Map<String, Object> pd) {
        if (snap.getSelfEntityId().isEmpty()) {
            return false;
        }
        Object phase = pd.get(CombatCycleKeys.KEY_COMBAT_PHASE);
        boolean engaged = CombatCycleKeys.PHASE_ENGAGED.equals(phase);
        boolean noTarget = !snap.getCurrentTargetEntityId().isPresent();
        return !engaged || noTarget;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings settings) {
        IWeaponSwapCoordinator coord = swapCoordinator;
        if (coord == null || settings == null) {
            return BehaviorStatus.SKIPPED;
        }
        Optional<ICombatSnapshot> snapOpt = combatModel.snapshot(context.getMachineId());
        if (!snapOpt.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        ICombatSnapshot snap = snapOpt.get();
        Map<String, Object> pd = context.getPersistentData();
        List<Integer> rot = settings.getBuffCasterSkillRotation();
        long timeoutMs = Math.max(500L, settings.getWeaponSwapTimeoutMs());

        Object stepRaw = pd.get(PD_MAINTAIN_STEP);
        int step = stepRaw instanceof Integer ? ((Integer) stepRaw).intValue() : 0;

        try {
            if (step == 0) {
                if (!idlePreconditions(context, snap, pd)) {
                    return BehaviorStatus.SKIPPED;
                }
                coord.swapTo(context, WeaponLoadout.BUFF_CASTER).get(timeoutMs * 3L, TimeUnit.MILLISECONDS);
                pd.put(PD_MAINTAIN_STEP, Integer.valueOf(STEP_CAST));
                pd.put(PD_MAINTAIN_IDX, Integer.valueOf(0));
                return BehaviorStatus.EXECUTED;
            }
            if (step == STEP_CAST) {
                int idx = 0;
                Object rawIdx = pd.get(PD_MAINTAIN_IDX);
                if (rawIdx instanceof Number) {
                    idx = ((Number) rawIdx).intValue();
                }
                if (idx >= rot.size()) {
                    pd.put(PD_MAINTAIN_STEP, Integer.valueOf(STEP_SWAP_BACK));
                    return BehaviorStatus.EXECUTED;
                }
                int skillRef = rot.get(idx).intValue();
                snap = combatModel.snapshot(context.getMachineId()).orElse(snap);
                int selfId = snap.getSelfEntityId().orElse(0);
                if (selfId > 0 && !buffPresent(snap, skillRef)) {
                    CombatPackets.sendSkillCast(context, skillRef, selfId);
                }
                pd.put(PD_MAINTAIN_IDX, Integer.valueOf(idx + 1));
                return BehaviorStatus.EXECUTED;
            }
            if (step == STEP_SWAP_BACK) {
                coord.swapTo(context, WeaponLoadout.MAIN_DAMAGE).get(timeoutMs * 3L, TimeUnit.MILLISECONDS);
                pd.remove(PD_MAINTAIN_STEP);
                pd.remove(PD_MAINTAIN_IDX);
                return BehaviorStatus.EXECUTED;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            clearMaintain(pd);
            return BehaviorStatus.SKIPPED;
        } catch (ExecutionException | TimeoutException e) {
            log.debug("MaintainBuffs failed: {}", e.toString());
            clearMaintain(pd);
            return BehaviorStatus.SKIPPED;
        }
        clearMaintain(pd);
        return BehaviorStatus.SKIPPED;
    }

    private static void clearMaintain(Map<String, Object> pd) {
        pd.remove(PD_MAINTAIN_STEP);
        pd.remove(PD_MAINTAIN_IDX);
    }

    private static boolean buffPresent(ICombatSnapshot snap, int skillRefId) {
        for (ActiveBuff b : snap.getActiveBuffs()) {
            if (b.getSkillRefId() == skillRefId) {
                long exp = b.getExpiresAtEpochMs();
                return exp == Long.MAX_VALUE || exp > System.currentTimeMillis();
            }
        }
        return false;
    }

    @Override
    public long postDelayMs() {
        return 300L;
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public int interruptionPriority() {
        return 350;
    }
}
