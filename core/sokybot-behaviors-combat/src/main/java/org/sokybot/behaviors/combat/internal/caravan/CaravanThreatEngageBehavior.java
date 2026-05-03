package org.sokybot.behaviors.combat.internal.caravan;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.CombatPackets;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.api.MonsterRef;
import org.sokybot.combat.projections.api.ICombatModel;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.swarm.api.CaravanBlackboardKeys;

/**
 * Epic #18: selects the Trader-reported threat entity on the Hunter client before normal farming search.
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE, property = "order=5")
public final class CaravanThreatEngageBehavior implements IBehavior<CombatSettings> {

    @Reference
    private ICombatModel combatModel;

    @Override
    public String id() {
        return "caravan-threat-engage";
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
        if (combatModel == null) {
            return false;
        }
        Object rawRef = context.getPersistentData().get(CaravanBlackboardKeys.KEY_THREAT_REF_ID);
        if (!(rawRef instanceof Number)) {
            return false;
        }
        int threatRef = ((Number) rawRef).intValue();
        if (threatRef <= 0) {
            return false;
        }
        Object rawUntil = context.getPersistentData().get(CaravanBlackboardKeys.KEY_DEFEND_UNTIL_MS);
        if (!(rawUntil instanceof Number)) {
            return false;
        }
        long defendUntil = ((Number) rawUntil).longValue();
        if (System.currentTimeMillis() >= defendUntil) {
            return false;
        }
        Optional<ICombatSnapshot> snap = combatModel.snapshot(context.getMachineId());
        if (!snap.isPresent()) {
            return false;
        }
        return snap.get().getCurrentTargetEntityId().isEmpty();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings settings) {
        if (combatModel == null) {
            return BehaviorStatus.SKIPPED;
        }
        Object rawRef = context.getPersistentData().get(CaravanBlackboardKeys.KEY_THREAT_REF_ID);
        Object rawPos = context.getPersistentData().get(CaravanBlackboardKeys.KEY_THREAT_POS);
        if (!(rawRef instanceof Number) || !(rawPos instanceof WorldPoint)) {
            return BehaviorStatus.SKIPPED;
        }
        int threatRef = ((Number) rawRef).intValue();
        WorldPoint threatPos = (WorldPoint) rawPos;

        Optional<ICombatSnapshot> snapOpt = combatModel.snapshot(context.getMachineId());
        if (!snapOpt.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        ICombatSnapshot snap = snapOpt.get();

        MonsterRef best = null;
        float bestDistSq = Float.MAX_VALUE;
        for (MonsterRef m : snap.getNearbyMonsters()) {
            if (m == null || m.getRefObjId() != threatRef) {
                continue;
            }
            float dx = m.getX() - threatPos.getX();
            float dy = m.getY() - threatPos.getY();
            float dz = m.getZ() - threatPos.getZ();
            float d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < bestDistSq) {
                bestDistSq = d2;
                best = m;
            }
        }
        if (best == null) {
            return BehaviorStatus.SKIPPED;
        }

        CombatPackets.sendSelectEntity(context, best.getEntityId());
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
}
