package org.sokybot.behaviors.navigation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.navigation.api.INavigator;
import org.sokybot.navigation.api.NavigationException;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.swarm.api.SwarmLureBlackboardKeys;
import org.sokybot.swarm.api.SwarmLureCycleEvent;
import org.sokybot.swarm.api.SwarmLurePhase;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.projections.api.ITownModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronized fan-out and converge-to-anchor movement for assigned swarm lurers (Epic #14).
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE, property = "order=0")
public final class SynchronizedLureBehavior implements IBehavior<SwarmLureSettings> {

    private static final Logger log = LoggerFactory.getLogger(SynchronizedLureBehavior.class);

    @Reference
    private SwarmLureDispatchListener dispatchListener;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile INavigator navigator;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITownModel townModel;

    @Override
    public String id() {
        return "swarmLure.synchronized";
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return SwarmLureCycleKeys.CYCLE_NAME.equals(cycleId);
    }

    @Override
    public Class<SwarmLureSettings> settingsType() {
        return SwarmLureSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, SwarmLureSettings settings) {
        if (settings == null || !settings.isSwarmLureCycleEnabled() || navigator == null) {
            return false;
        }
        String machineId = context.getMachineId();
        if (dispatchListener.hasPending(machineId)) {
            return true;
        }
        return context.getPersistentData().get(SwarmLureBlackboardKeys.KEY_LURE_REQUEST_ID) != null;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, SwarmLureSettings settings) {
        INavigator nav = navigator;
        if (settings == null || !settings.isSwarmLureCycleEnabled() || nav == null) {
            return BehaviorStatus.SKIPPED;
        }

        String machineId = context.getMachineId();
        Map<String, Object> pd = context.getPersistentData();

        SwarmLureCycleEvent latest = drainLatestEventFromListener(machineId);
        if (latest != null) {
            captureEvent(latest, machineId, pd);
        }

        if (pd.get(SwarmLureBlackboardKeys.KEY_LURE_REQUEST_ID) == null) {
            return BehaviorStatus.SKIPPED;
        }

        SwarmLurePhase phase = readPhase(pd);
        if (phase == null) {
            return BehaviorStatus.SKIPPED;
        }

        long now = System.currentTimeMillis();
        try {
            switch (phase) {
                case FAN_OUT:
                    return executeFanOut(context, settings, nav, pd, now);
                case PULLING:
                    return BehaviorStatus.SKIPPED;
                case CONVERGING:
                    return executeConverging(context, settings, nav, pd, now);
                case ARRIVED:
                default:
                    return BehaviorStatus.SKIPPED;
            }
        } catch (NavigationException e) {
            log.debug("Swarm lure navigation failed: {}", e.getMessage());
            return BehaviorStatus.SKIPPED;
        }
    }

    private SwarmLureCycleEvent drainLatestEventFromListener(String machineId) {
        SwarmLureCycleEvent latest = null;
        for (;;) {
            SwarmLureCycleEvent n = dispatchListener.poll(machineId);
            if (n == null) {
                break;
            }
            latest = n;
        }
        return latest;
    }

    private void captureEvent(SwarmLureCycleEvent e, String machineId, Map<String, Object> pd) {
        List<String> assigned = e.getAssignedLurers();
        int slice = assigned.indexOf(machineId);
        if (slice < 0) {
            log.warn("Swarm lure event request {} has no slice for machine {}", e.getRequestId(), machineId);
            return;
        }

        String prevRid = stringFrom(pd.get(SwarmLureBlackboardKeys.KEY_LURE_REQUEST_ID));
        if (!e.getRequestId().equals(prevRid)) {
            pd.put(SwarmLureBlackboardKeys.KEY_FAN_OUT_COMPLETE, Boolean.FALSE);
        }

        WorldPoint a = e.getAnchorPoint();
        pd.put(SwarmLureBlackboardKeys.KEY_LURE_REQUEST_ID, e.getRequestId());
        pd.put(SwarmLureBlackboardKeys.KEY_LURE_PHASE, e.getPhase().name());
        pd.put(SwarmLureBlackboardKeys.KEY_SYNC_EPOCH_MS, Long.valueOf(e.getSyncEpochMs()));
        pd.put(SwarmLureBlackboardKeys.KEY_AGREED_TTT_MS, Long.valueOf(e.getAgreedTimeToTargetMs()));
        pd.put(SwarmLureBlackboardKeys.KEY_SLICE_INDEX, Integer.valueOf(slice));
        pd.put(SwarmLureBlackboardKeys.KEY_LURER_COUNT, Integer.valueOf(assigned.size()));
        pd.put(SwarmLureBlackboardKeys.KEY_ANCHOR_MACHINE_ID, e.getAnchorMachineId());
        pd.put(SwarmLureBlackboardKeys.KEY_ANCHOR_X, Float.valueOf(a.getX()));
        pd.put(SwarmLureBlackboardKeys.KEY_ANCHOR_Y, Float.valueOf(a.getY()));
        pd.put(SwarmLureBlackboardKeys.KEY_ANCHOR_Z, Float.valueOf(a.getZ()));
        pd.put(SwarmLureBlackboardKeys.KEY_FAN_RADIUS_WORLD, Float.valueOf(e.getFanRadiusWorld()));

        WorldPoint fan = computeFanWaypoint(a, e.getFanRadiusWorld(), slice, assigned.size());
        if (fan != null) {
            pd.put(SwarmLureBlackboardKeys.KEY_FAN_X, Float.valueOf(fan.getX()));
            pd.put(SwarmLureBlackboardKeys.KEY_FAN_Y, Float.valueOf(fan.getY()));
            pd.put(SwarmLureBlackboardKeys.KEY_FAN_Z, Float.valueOf(fan.getZ()));
        }
    }

    private static SwarmLurePhase readPhase(Map<String, Object> pd) {
        Object raw = pd.get(SwarmLureBlackboardKeys.KEY_LURE_PHASE);
        if (!(raw instanceof String)) {
            return null;
        }
        try {
            return SwarmLurePhase.valueOf((String) raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private BehaviorStatus executeFanOut(
            IWorkflowContext context,
            SwarmLureSettings settings,
            INavigator nav,
            Map<String, Object> pd,
            long now) throws NavigationException {
        if (Boolean.TRUE.equals(pd.get(SwarmLureBlackboardKeys.KEY_FAN_OUT_COMPLETE))) {
            return BehaviorStatus.SKIPPED;
        }
        WorldPoint fan = computeFanWaypointFromPersistent(pd);
        if (fan == null) {
            return BehaviorStatus.SKIPPED;
        }
        pd.put(SwarmLureBlackboardKeys.KEY_FAN_X, Float.valueOf(fan.getX()));
        pd.put(SwarmLureBlackboardKeys.KEY_FAN_Y, Float.valueOf(fan.getY()));
        pd.put(SwarmLureBlackboardKeys.KEY_FAN_Z, Float.valueOf(fan.getZ()));
        Optional<WorldPoint> me = selfPosition(context);
        if (!me.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        if (horizontalDistance(me.get(), fan) <= settings.getArrivalRadiusWorld()) {
            pd.put(SwarmLureBlackboardKeys.KEY_FAN_OUT_COMPLETE, Boolean.TRUE);
            pd.put(SwarmLureBlackboardKeys.KEY_LAST_NAV_ISSUED_AT_MS, Long.valueOf(now));
            return BehaviorStatus.EXECUTED;
        }
        nav.walkTo(context, fan);
        pd.put(SwarmLureBlackboardKeys.KEY_LAST_NAV_ISSUED_AT_MS, Long.valueOf(now));
        return BehaviorStatus.EXECUTED;
    }

    private BehaviorStatus executeConverging(
            IWorkflowContext context,
            SwarmLureSettings settings,
            INavigator nav,
            Map<String, Object> pd,
            long now) throws NavigationException {
        long tSync = longFrom(pd.get(SwarmLureBlackboardKeys.KEY_SYNC_EPOCH_MS));
        WorldPoint anchor = readAnchorPoint(pd);
        if (anchor == null || tSync <= 0L) {
            return BehaviorStatus.SKIPPED;
        }

        Optional<WorldPoint> meOpt = selfPosition(context);
        if (!meOpt.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        WorldPoint me = meOpt.get();

        float arriveR = settings.getArrivalRadiusWorld();
        double dist = horizontalDistance(me, anchor);
        if (dist <= arriveR) {
            return BehaviorStatus.EXECUTED;
        }

        float speed = Math.max(0.5f, settings.getAssumedWalkSpeedWorldPerSec());
        long tRemainMs = (long) Math.ceil((dist / speed) * 1000.0);
        long buffer = Math.max(0L, settings.getSyncSafetyBufferMs());
        long tStart = tSync - tRemainMs - buffer;

        if (now < tStart) {
            return BehaviorStatus.EXECUTED;
        }

        nav.walkTo(context, anchor);
        pd.put(SwarmLureBlackboardKeys.KEY_CONVERGE_ARMED, Boolean.TRUE);
        pd.put(SwarmLureBlackboardKeys.KEY_LAST_NAV_ISSUED_AT_MS, Long.valueOf(now));
        return BehaviorStatus.EXECUTED;
    }

    private Optional<WorldPoint> selfPosition(IWorkflowContext context) {
        String mid = context.getMachineId();
        ITownModel model = townModel;
        if (model != null) {
            Optional<ITownSnapshot> snap = model.snapshot(mid);
            if (snap.isPresent() && !snap.get().isDead()) {
                ITownSnapshot s = snap.get();
                return Optional.of(new WorldPoint(s.getSelfX(), s.getSelfY(), s.getSelfZ()));
            }
        }
        if (context.getGameModel() == null || context.getGameModel().getTrainer() == null) {
            return Optional.empty();
        }
        GamePosition p = context.getGameModel().getTrainer().getPosition();
        if (p == null) {
            return Optional.empty();
        }
        return Optional.of(new WorldPoint(p.getX(), p.getY(), p.getZ()));
    }

    private static WorldPoint readAnchorPoint(Map<String, Object> pd) {
        Object x = pd.get(SwarmLureBlackboardKeys.KEY_ANCHOR_X);
        Object y = pd.get(SwarmLureBlackboardKeys.KEY_ANCHOR_Y);
        Object z = pd.get(SwarmLureBlackboardKeys.KEY_ANCHOR_Z);
        if (!(x instanceof Number) || !(y instanceof Number) || !(z instanceof Number)) {
            return null;
        }
        return new WorldPoint(
                ((Number) x).floatValue(),
                ((Number) y).floatValue(),
                ((Number) z).floatValue());
    }

    private static WorldPoint computeFanWaypointFromPersistent(Map<String, Object> pd) {
        WorldPoint anchor = readAnchorPoint(pd);
        if (anchor == null) {
            return null;
        }
        int slice = intFrom(pd.get(SwarmLureBlackboardKeys.KEY_SLICE_INDEX));
        int count = intFrom(pd.get(SwarmLureBlackboardKeys.KEY_LURER_COUNT));
        float radius = floatFrom(pd.get(SwarmLureBlackboardKeys.KEY_FAN_RADIUS_WORLD));
        return computeFanWaypoint(anchor, radius, slice, count);
    }

    private static WorldPoint computeFanWaypoint(WorldPoint anchor, float fanRadiusWorld, int sliceIndex, int lurerCount) {
        if (anchor == null || lurerCount <= 0 || fanRadiusWorld <= 0f || sliceIndex < 0 || sliceIndex >= lurerCount) {
            return null;
        }
        double theta = -Math.PI / 2.0 + (2.0 * Math.PI * sliceIndex / lurerCount);
        float x = anchor.getX() + fanRadiusWorld * (float) Math.cos(theta);
        float y = anchor.getY() + fanRadiusWorld * (float) Math.sin(theta);
        float z = anchor.getZ();
        return new WorldPoint(x, y, z);
    }

    private static double horizontalDistance(WorldPoint a, WorldPoint b) {
        float dx = a.getX() - b.getX();
        float dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static long longFrom(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return 0L;
    }

    private static String stringFrom(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static int intFrom(Object o) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return 0;
    }

    private static float floatFrom(Object o) {
        if (o instanceof Number) {
            return ((Number) o).floatValue();
        }
        return 0f;
    }
}
