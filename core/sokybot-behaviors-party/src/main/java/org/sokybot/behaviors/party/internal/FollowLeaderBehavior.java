package org.sokybot.behaviors.party.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.commons.SilkroadUtils;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.projections.api.ICombatModel;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.navigation.api.IPathfinder;
import org.sokybot.navigation.api.INavigator;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.party.api.IPartyModel;
import org.sokybot.party.api.IPartyPolicy;
import org.sokybot.party.api.IPartySnapshot;
import org.sokybot.party.api.PartyCycleKeys;
import org.sokybot.party.api.PartyMember;
import org.sokybot.party.api.PartyRole;

@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class FollowLeaderBehavior implements IBehavior<CombatSettings> {

    private static final long MOVE_COOLDOWN_MS = 450L;

    @Reference
    private ICombatModel combatModel;

    @Reference
    private IPartyModel partyModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IPathfinder pathfinder;

    @SuppressWarnings("unused")
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile INavigator navigator;

    @Override
    public String id() {
        return PartyCycleKeys.BEHAVIOR_FOLLOW_LEADER;
    }

    @Override
    public int order() {
        return 20;
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
        IPartyPolicy policy = PartyPolicyResolver.resolve(context);
        if (!policy.isFollowLeader()) {
            return false;
        }
        Optional<ICombatSnapshot> c = combatModel.snapshot(context.getMachineId());
        Optional<IPartySnapshot> p = partyModel.snapshot(context.getMachineId());
        if (!c.isPresent() || !p.isPresent()) {
            return false;
        }
        return shouldMove(c.get(), p.get(), policy) != null;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings settings) {
        IPartyPolicy policy = PartyPolicyResolver.resolve(context);
        if (!policy.isFollowLeader()) {
            return BehaviorStatus.SKIPPED;
        }
        Optional<ICombatSnapshot> cOpt = combatModel.snapshot(context.getMachineId());
        Optional<IPartySnapshot> pOpt = partyModel.snapshot(context.getMachineId());
        if (!cOpt.isPresent() || !pOpt.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        ICombatSnapshot c = cOpt.get();
        IPartySnapshot p = pOpt.get();
        WorldPoint step = shouldMove(c, p, policy);
        if (step == null) {
            return BehaviorStatus.SKIPPED;
        }
        Map<String, Object> pd = context.getPersistentData();
        long now = System.currentTimeMillis();
        Object raw = pd.get(PartyCycleKeys.KEY_LAST_PARTY_FOLLOW_AT_MS);
        long last = raw instanceof Number ? ((Number) raw).longValue() : 0L;
        if (now - last < MOVE_COOLDOWN_MS) {
            return BehaviorStatus.SKIPPED;
        }

        float gx = step.getX();
        float gy = step.getY();
        float gz = step.getZ();
        short sx = SilkroadUtils.getSectorX(gx);
        byte sy = SilkroadUtils.getSectorY(gy);
        PartyPackets.sendCharMove(context, gx, gy, gz, sx & 0xFFFF, sy & 0xFF);
        pd.put(PartyCycleKeys.KEY_LAST_PARTY_FOLLOW_AT_MS, Long.valueOf(now));
        return BehaviorStatus.EXECUTED;
    }

    /**
     * Returns movement waypoint toward leader when beyond follow distance.
     */
    private WorldPoint shouldMove(ICombatSnapshot combat, IPartySnapshot party, IPartyPolicy policy) {
        Optional<Integer> selfOpt = combat.getSelfEntityId();
        if (!selfOpt.isPresent()) {
            return null;
        }
        int selfId = selfOpt.get().intValue();
        int leaderId = party.getLeaderEntityId();
        if (leaderId <= 0 || leaderId == selfId) {
            return null;
        }
        PartyMember leader = findMember(party.getMembers(), leaderId);
        if (leader == null) {
            return null;
        }
        WorldPoint origin = new WorldPoint(combat.getSelfX(), combat.getSelfY(), combat.getSelfZ());
        WorldPoint dest = new WorldPoint(leader.getX(), leader.getY(), leader.getZ());
        float maxDist = Math.max(1f, policy.getFollowDistanceWorld());
        if (origin.distanceTo(dest) <= maxDist) {
            return null;
        }

        IPathfinder pf = pathfinder;
        if (pf != null) {
            List<WorldPoint> path = pf.findPath(origin, dest);
            if (path != null && path.size() >= 2) {
                return path.get(1);
            }
        }

        return dest;
    }

    private static PartyMember findMember(List<PartyMember> members, int entityId) {
        if (members == null) {
            return null;
        }
        for (PartyMember m : members) {
            if (m != null && m.getEntityId() == entityId && m.getRole() == PartyRole.LEADER) {
                return m;
            }
        }
        for (PartyMember m : members) {
            if (m != null && m.getEntityId() == entityId) {
                return m;
            }
        }
        return null;
    }

    @Override
    public long postDelayMs() {
        return 200L;
    }
}
