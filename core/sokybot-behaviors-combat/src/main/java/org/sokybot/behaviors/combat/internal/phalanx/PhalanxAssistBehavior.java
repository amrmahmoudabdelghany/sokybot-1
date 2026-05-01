package org.sokybot.behaviors.combat.internal.phalanx;

import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.CombatPackets;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.combat.api.ICombatPolicy;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.api.MonsterRef;
import org.sokybot.combat.projections.api.ICombatModel;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.swarm.api.SwarmTargetEngagedEvent;

@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE, property = {
        "order=4"
})
public final class PhalanxAssistBehavior implements IBehavior<CombatSettings> {

    @Reference
    private ICombatModel combatModel;

    @Reference
    private PhalanxTargetListener listener;

    @Override
    public String id() {
        return "combat.phalanxAssist";
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
        Optional<ICombatSnapshot> snap = combatModel.snapshot(context.getMachineId());
        if (!snap.isPresent() || settings == null || !settings.isPhalanxFollowerEnabled()) {
            return false;
        }
        if (snap.get().isSkillCastInFlight()) {
            return false;
        }
        return listener.peekFresh(context.getMachineId(), settings.getPhalanxStaleEventTimeoutMs()).isPresent();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings settings) {
        Optional<ICombatSnapshot> snapOpt = combatModel.snapshot(context.getMachineId());
        if (!snapOpt.isPresent() || settings == null) {
            return BehaviorStatus.SKIPPED;
        }
        ICombatSnapshot snap = snapOpt.get();
        if (snap.isSkillCastInFlight()) {
            return BehaviorStatus.SKIPPED;
        }
        Optional<SwarmTargetEngagedEvent> evtOpt = listener.peekFresh(context.getMachineId(),
                settings.getPhalanxStaleEventTimeoutMs());
        if (!evtOpt.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        SwarmTargetEngagedEvent evt = evtOpt.get();
        MonsterRef target = findNearby(snap, evt.getTargetEntityId());
        if (target == null) {
            return BehaviorStatus.SKIPPED;
        }
        ICombatPolicy policy = settings.toPolicy();
        float maxAllowed = Math.min(policy.getMaxEngageDistance(), settings.getPhalanxMaxRangeWorld());
        if (distance(snap.getSelfX(), snap.getSelfY(), snap.getSelfZ(), target.getX(), target.getY(),
                target.getZ()) > maxAllowed) {
            return BehaviorStatus.SKIPPED;
        }
        if (!snap.getCurrentTargetEntityId().isPresent()
                || snap.getCurrentTargetEntityId().get().intValue() != target.getEntityId()) {
            CombatPackets.sendSelectEntity(context, target.getEntityId());
        }
        if (evt.getKind() == SwarmTargetEngagedEvent.Kind.ATTACK_SKILL && evt.getSkillRefId() > 0
                && skillReady(snap.getSkillCooldownReadyAtEpochMs(), evt.getSkillRefId(), System.currentTimeMillis())) {
            CombatPackets.sendSkillCast(context, evt.getSkillRefId(), target.getEntityId());
        } else {
            CombatPackets.sendCharActionAttack(context, target.getEntityId());
        }
        context.getPersistentData().put(CombatCycleKeys.KEY_COMBAT_PHASE, CombatCycleKeys.PHASE_ENGAGED);
        context.getPersistentData().put(CombatCycleKeys.KEY_LAST_SKILL_EXECUTED_AT_MS,
                Long.valueOf(System.currentTimeMillis()));
        return BehaviorStatus.EXECUTED;
    }

    private static MonsterRef findNearby(ICombatSnapshot snap, int targetEntityId) {
        for (MonsterRef m : snap.getNearbyMonsters()) {
            if (m.getEntityId() == targetEntityId) {
                return m;
            }
        }
        return null;
    }

    private static boolean skillReady(Map<Integer, Long> readyBySkillRef, int skillRefId, long now) {
        Long readyAt = readyBySkillRef.get(Integer.valueOf(skillRefId));
        return readyAt == null || readyAt.longValue() <= now;
    }

    private static float distance(float x1, float y1, float z1, float x2, float y2, float z2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        float dz = z1 - z2;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
