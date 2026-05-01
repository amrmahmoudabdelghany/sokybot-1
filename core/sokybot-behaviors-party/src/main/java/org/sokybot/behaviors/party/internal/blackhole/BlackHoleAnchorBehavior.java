package org.sokybot.behaviors.party.internal.blackhole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.CombatPackets;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.projections.api.ICombatModel;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmLureBlackboardKeys;
import org.sokybot.swarm.api.SwarmLureCycleEvent;
import org.sokybot.swarm.api.SwarmLurePhase;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.projections.api.ITownModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nuker anchor: suppresses ordinary engage during lure coordination, publishes CONVERGING with computed TTT, then nukes
 * at sync epoch (Epic #14).
 */
@Component(
        service = IBehavior.class,
        immediate = true,
        scope = ServiceScope.PROTOTYPE,
        property = "order=5")
public final class BlackHoleAnchorBehavior implements IBehavior<CombatSettings> {

    private static final Logger log = LoggerFactory.getLogger(BlackHoleAnchorBehavior.class);

    static final String SETTINGS_SCOPE_BLACK_HOLE = "blackHole";

    @Reference
    private BlackHoleLureBusListener lureListener;

    @Reference
    private ISwarmEventBus swarmBus;

    @Reference
    private ICombatModel combatModel;

    @Reference
    private ISettingsRegistry settingsRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITownModel townModel;

    @Override
    public String id() {
        return "party.blackHoleAnchor";
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
    public boolean applies(IWorkflowContext context, CombatSettings combatSettings) {
        BlackHoleAnchorSettings bh = resolveBlackHoleSettings(context);
        if (bh == null || !bh.isBlackHoleAnchorEnabled()) {
            return false;
        }
        String mid = context.getMachineId();
        Map<String, Object> pd = context.getPersistentData();
        Optional<SwarmLureCycleEvent> latest = lureListener.latestForAnchor(mid);
        if (!latest.isPresent()) {
            return false;
        }
        SwarmLurePhase ph = latest.get().getPhase();
        if (ph == SwarmLurePhase.ARRIVED && Boolean.TRUE.equals(pd.get(SwarmLureBlackboardKeys.KEY_ANCHOR_NUKE_FIRED))) {
            return false;
        }
        return ph == SwarmLurePhase.FAN_OUT
                || ph == SwarmLurePhase.PULLING
                || ph == SwarmLurePhase.CONVERGING
                || ph == SwarmLurePhase.ARRIVED;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings combatSettings) {
        BlackHoleAnchorSettings bh = resolveBlackHoleSettings(context);
        if (bh == null || !bh.isBlackHoleAnchorEnabled()) {
            return BehaviorStatus.SKIPPED;
        }
        String mid = context.getMachineId();
        Map<String, Object> pd = context.getPersistentData();
        Optional<SwarmLureCycleEvent> latestOpt = lureListener.latestForAnchor(mid);
        if (!latestOpt.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        SwarmLureCycleEvent latest = latestOpt.get();
        syncRequestState(pd, latest.getRequestId());

        long now = System.currentTimeMillis();
        SwarmLurePhase phase = latest.getPhase();

        switch (phase) {
            case FAN_OUT:
                pd.put(SwarmLureBlackboardKeys.KEY_ANCHOR_SUPPRESS_ENGAGE, Boolean.TRUE);
                return BehaviorStatus.EXECUTED;
            case PULLING:
                pd.put(SwarmLureBlackboardKeys.KEY_ANCHOR_SUPPRESS_ENGAGE, Boolean.TRUE);
                if (pd.get(SwarmLureBlackboardKeys.KEY_ANCHOR_PULL_START_EPOCH_MS) == null) {
                    pd.put(SwarmLureBlackboardKeys.KEY_ANCHOR_PULL_START_EPOCH_MS, Long.valueOf(now));
                }
                long pullStart = longFrom(pd.get(SwarmLureBlackboardKeys.KEY_ANCHOR_PULL_START_EPOCH_MS));
                boolean convergePublished = Boolean.TRUE.equals(pd.get(SwarmLureBlackboardKeys.KEY_ANCHOR_CONVERGE_PUBLISHED));
                if (!convergePublished && pullStart > 0L && now - pullStart >= Math.max(0L, bh.getPullingPhaseDurationMs())) {
                    publishConverging(context, bh, latest, pd, now);
                }
                return BehaviorStatus.EXECUTED;
            case CONVERGING:
                pd.put(SwarmLureBlackboardKeys.KEY_ANCHOR_SUPPRESS_ENGAGE, Boolean.TRUE);
                long syncEpoch = longFrom(pd.get(SwarmLureBlackboardKeys.KEY_ANCHOR_PUBLISHED_SYNC_EPOCH_MS));
                if (syncEpoch <= 0L) {
                    syncEpoch = latest.getSyncEpochMs();
                }
                if (now < syncEpoch) {
                    return BehaviorStatus.EXECUTED;
                }
                if (!Boolean.TRUE.equals(pd.get(SwarmLureBlackboardKeys.KEY_ANCHOR_NUKE_FIRED))) {
                    fireNukeAndArrived(context, bh, latest, pd, now, syncEpoch);
                }
                return BehaviorStatus.EXECUTED;
            case ARRIVED:
                pd.remove(SwarmLureBlackboardKeys.KEY_ANCHOR_SUPPRESS_ENGAGE);
                return BehaviorStatus.SKIPPED;
            default:
                return BehaviorStatus.SKIPPED;
        }
    }

    private void publishConverging(
            IWorkflowContext context,
            BlackHoleAnchorSettings bh,
            SwarmLureCycleEvent latest,
            Map<String, Object> pd,
            long now) {
        Optional<WorldPoint> anchorOpt = selfAnchorPoint(context);
        if (!anchorOpt.isPresent()) {
            log.warn("Black hole anchor: no self position; skip CONVERGING publish");
            return;
        }
        WorldPoint anchor = anchorOpt.get();
        List<String> assigned = mergeAssignedRoster(bh, latest);
        List<WorldPoint> lurerPts = resolveLurerWorldPoints(bh, anchor, assigned);

        float speed = Math.max(0.5f, bh.getTttAssumedWalkSpeedWorldPerSec());
        long tttMs = BlackHoleTttPlanner.computeTttMs(anchor, lurerPts, speed, bh.getKillRadiusWorld());
        long syncEpochMs = now + tttMs + Math.max(0L, bh.getSyncFudgeMs());

        SwarmLureCycleEvent out = new SwarmLureCycleEvent(
                context.getMachineId(),
                now,
                latest.getRequestId(),
                SwarmLurePhase.CONVERGING,
                syncEpochMs,
                tttMs,
                context.getMachineId(),
                anchor,
                bh.getFanRadiusWorld(),
                assigned);
        swarmBus.publish(out);
        pd.put(SwarmLureBlackboardKeys.KEY_ANCHOR_CONVERGE_PUBLISHED, Boolean.TRUE);
        pd.put(SwarmLureBlackboardKeys.KEY_ANCHOR_PUBLISHED_SYNC_EPOCH_MS, Long.valueOf(syncEpochMs));
    }

    private static List<String> mergeAssignedRoster(BlackHoleAnchorSettings bh, SwarmLureCycleEvent latest) {
        List<String> out = new ArrayList<>();
        if (bh.getAssignedLurerMachineIds() != null) {
            for (String id : bh.getAssignedLurerMachineIds()) {
                if (id != null && !id.trim().isEmpty()) {
                    out.add(id.trim());
                }
            }
        }
        if (out.isEmpty() && latest.getAssignedLurers() != null) {
            out.addAll(latest.getAssignedLurers());
        }
        return out.isEmpty() ? Collections.emptyList() : out;
    }

    private List<WorldPoint> resolveLurerWorldPoints(BlackHoleAnchorSettings bh, WorldPoint anchor, List<String> assigned) {
        List<WorldPoint> resolved = new ArrayList<>();
        if (assigned == null || assigned.isEmpty()) {
            return resolved;
        }
        ITownModel town = townModel;
        for (String machineId : assigned) {
            if (machineId == null || machineId.trim().isEmpty()) {
                continue;
            }
            String mid = machineId.trim();
            Optional<WorldPoint> wp = Optional.empty();
            if (town != null) {
                Optional<ITownSnapshot> ts = town.snapshot(mid);
                if (ts.isPresent() && !ts.get().isDead()) {
                    ITownSnapshot s = ts.get();
                    wp = Optional.of(new WorldPoint(s.getSelfX(), s.getSelfY(), s.getSelfZ()));
                }
            }
            if (!wp.isPresent()) {
                Optional<ICombatSnapshot> cs = combatModel.snapshot(mid);
                if (cs.isPresent()) {
                    ICombatSnapshot s = cs.get();
                    wp = Optional.of(new WorldPoint(s.getSelfX(), s.getSelfY(), s.getSelfZ()));
                }
            }
            if (wp.isPresent()) {
                resolved.add(wp.get());
            }
        }
        if (resolved.size() != assigned.size()) {
            return ringAtFanRadius(anchor, bh.getFanRadiusWorld(), assigned.size());
        }
        return resolved;
    }

    private static List<WorldPoint> ringAtFanRadius(WorldPoint anchor, float fanRadiusWorld, int count) {
        List<WorldPoint> ring = new ArrayList<>();
        if (anchor == null || count <= 0 || fanRadiusWorld <= 0f) {
            return ring;
        }
        for (int i = 0; i < count; i++) {
            double theta = -Math.PI / 2.0 + (2.0 * Math.PI * i / count);
            ring.add(new WorldPoint(
                    anchor.getX() + fanRadiusWorld * (float) Math.cos(theta),
                    anchor.getY() + fanRadiusWorld * (float) Math.sin(theta),
                    anchor.getZ()));
        }
        return ring;
    }

    private void fireNukeAndArrived(
            IWorkflowContext context,
            BlackHoleAnchorSettings bh,
            SwarmLureCycleEvent latest,
            Map<String, Object> pd,
            long now,
            long syncEpochMs) {
        Optional<WorldPoint> anchorOpt = selfAnchorPoint(context);
        WorldPoint anchor = anchorOpt.orElseGet(latest::getAnchorPoint);
        List<String> assigned = mergeAssignedRoster(bh, latest);
        long tttMs = latest.getAgreedTimeToTargetMs();

        SwarmLureCycleEvent arrived = new SwarmLureCycleEvent(
                context.getMachineId(),
                now,
                latest.getRequestId(),
                SwarmLurePhase.ARRIVED,
                syncEpochMs,
                tttMs,
                context.getMachineId(),
                anchor,
                bh.getFanRadiusWorld(),
                assigned);
        swarmBus.publish(arrived);

        Optional<ICombatSnapshot> snap = combatModel.snapshot(context.getMachineId());
        int skill = bh.getNukeSkillRefId();
        if (snap.isPresent() && skill > 0) {
            int selfId = snap.get().getSelfEntityId().orElse(0);
            if (selfId > 0) {
                CombatPackets.sendSkillCast(context, skill, selfId);
            }
        }

        pd.put(SwarmLureBlackboardKeys.KEY_ANCHOR_NUKE_FIRED, Boolean.TRUE);
        pd.remove(SwarmLureBlackboardKeys.KEY_ANCHOR_SUPPRESS_ENGAGE);
    }

    private Optional<WorldPoint> selfAnchorPoint(IWorkflowContext context) {
        String mid = context.getMachineId();
        ITownModel town = townModel;
        if (town != null) {
            Optional<ITownSnapshot> ts = town.snapshot(mid);
            if (ts.isPresent() && !ts.get().isDead()) {
                ITownSnapshot s = ts.get();
                return Optional.of(new WorldPoint(s.getSelfX(), s.getSelfY(), s.getSelfZ()));
            }
        }
        Optional<ICombatSnapshot> cs = combatModel.snapshot(mid);
        if (cs.isPresent()) {
            ICombatSnapshot s = cs.get();
            return Optional.of(new WorldPoint(s.getSelfX(), s.getSelfY(), s.getSelfZ()));
        }
        return Optional.empty();
    }

    private static void syncRequestState(Map<String, Object> pd, String requestId) {
        if (requestId == null) {
            return;
        }
        String rid = requestId.trim();
        Object prev = pd.get(SwarmLureBlackboardKeys.KEY_ANCHOR_ACTIVE_REQUEST_ID);
        String prevStr = prev == null ? "" : String.valueOf(prev);
        if (rid.equals(prevStr)) {
            return;
        }
        pd.put(SwarmLureBlackboardKeys.KEY_ANCHOR_ACTIVE_REQUEST_ID, rid);
        pd.remove(SwarmLureBlackboardKeys.KEY_ANCHOR_PULL_START_EPOCH_MS);
        pd.put(SwarmLureBlackboardKeys.KEY_ANCHOR_CONVERGE_PUBLISHED, Boolean.FALSE);
        pd.put(SwarmLureBlackboardKeys.KEY_ANCHOR_NUKE_FIRED, Boolean.FALSE);
        pd.remove(SwarmLureBlackboardKeys.KEY_ANCHOR_PUBLISHED_SYNC_EPOCH_MS);
    }

    private BlackHoleAnchorSettings resolveBlackHoleSettings(IWorkflowContext ctx) {
        ISettingsRegistry reg = settingsRegistry;
        if (reg == null) {
            return null;
        }
        ISettingsProvider<BlackHoleAnchorSettings> provider = reg.getProvider(
                ctx.getGroupName(),
                ctx.getMachineName(),
                SETTINGS_SCOPE_BLACK_HOLE,
                BlackHoleAnchorSettings.class);
        if (provider == null) {
            return null;
        }
        try {
            return provider.get();
        } catch (Exception e) {
            return null;
        }
    }

    private static long longFrom(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return 0L;
    }
}
