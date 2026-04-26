package org.sokybot.behaviors.swarm.hunting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.events.world.GameNotifyEvent;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.swarm.api.HuntCompletedEvent;
import org.sokybot.swarm.api.HuntDispatchedEvent;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmEntityDetectedEvent;
import org.sokybot.topology.api.ITeleportRouter;
import org.sokybot.topology.api.TeleportRoute;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.projections.api.ITownModel;
import org.sokybot.trade.coordination.api.SwarmRole;

import reactor.core.Disposable;

@Component(immediate = true, service = UniqueHuntingCoordinator.class)
public final class UniqueHuntingCoordinator {

    @Reference
    private ISwarmEventBus swarmBus;

    @Reference
    private IReactiveEventBus eventBus;

    @Reference
    private ISokybotContext sokybotCtx;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITeleportRouter router;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITownModel townModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISettingsRegistry settingsRegistry;

    private final ConcurrentMap<String, ActiveHunt> activeHunts = new ConcurrentHashMap<>();

    private Disposable detectionSub;
    private Disposable killedSub;

    @Activate
    void activate() {
        detectionSub = swarmBus.observe(SwarmEntityDetectedEvent.class).subscribe(this::onDetection);
        killedSub = eventBus.on(GameNotifyEvent.class).subscribe(this::onNotify);
    }

    @Deactivate
    void deactivate() {
        dispose(detectionSub);
        dispose(killedSub);
        activeHunts.clear();
    }

    private static void dispose(Disposable d) {
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private void onNotify(GameNotifyEvent ev) {
        if (ev.getType() != GameNotifyEvent.NotifyType.UNIQUE_KILLED) {
            return;
        }
        List<ActiveHunt> completed = activeHunts.values().stream()
                .filter(h -> h.targetRefId == ev.getModelId())
                .collect(Collectors.toList());
        for (ActiveHunt hunt : completed) {
            if (activeHunts.remove(hunt.huntId, hunt)) {
                swarmBus.publish(new HuntCompletedEvent(
                        "coordinator",
                        System.currentTimeMillis(),
                        hunt.huntId,
                        hunt.huntId,
                        hunt.hunterMachineId,
                        hunt.targetRefId));
                swarmBus.releaseClaim(hunt.huntId);
            }
        }
    }

    private void onDetection(SwarmEntityDetectedEvent ev) {
        ITeleportRouter localRouter = router;
        ITownModel localTown = townModel;
        if (localRouter == null || localTown == null || ev.getCandidatePositions().isEmpty()) {
            return;
        }

        String flightKey = "hive-dispatch-" + ev.getRefId() + "-" + (ev.getTimestampEpochMs() / 30_000L);
        if (!swarmBus.tryClaim(flightKey, "coordinator")) {
            return;
        }
        try {
            Optional<Assignment> best = enumerateHunters(localTown).stream()
                    .flatMap(h -> ev.getCandidatePositions().stream()
                            .map(p -> assignmentFor(localRouter, h, p))
                            .filter(Optional::isPresent)
                            .map(Optional::get))
                    .min(Comparator.comparingLong(a -> a.etaMs));
            if (!best.isPresent()) {
                return;
            }

            Assignment a = best.get();
            String huntId = java.util.UUID.randomUUID().toString();
            activeHunts.put(huntId, new ActiveHunt(huntId, a.hunterMachineId, ev.getRefId()));
            swarmBus.publish(new HuntDispatchedEvent(
                    "coordinator",
                    System.currentTimeMillis(),
                    huntId,
                    huntId,
                    a.hunterMachineId,
                    ev.getRefId(),
                    a.targetPosition,
                    a.etaMs));
        } finally {
            swarmBus.releaseClaim(flightKey);
        }
    }

    private List<HunterCandidate> enumerateHunters(ITownModel localTown) {
        List<HunterCandidate> out = new ArrayList<>();
        for (IGroupContext group : sokybotCtx.getGroups()) {
            if (group == null) {
                continue;
            }
            for (IMachineContext machine : group.getMachines()) {
                if (machine == null || !machine.isRunning()) {
                    continue;
                }
                String machineId = machine.fullName();
                HuntingRuntimeSettings cfg = readSettings(group.name(), machine.getMachineName());
                if (!cfg.enabled || (cfg.role != SwarmRole.BOTH && cfg.role != SwarmRole.HUNTER)) {
                    continue;
                }
                if (activeHunts.values().stream().anyMatch(h -> h.hunterMachineId.equals(machineId))) {
                    continue;
                }
                ITownSnapshot snap = localTown.snapshot(machineId).orElse(null);
                if (snap == null || snap.isDead()) {
                    continue;
                }
                out.add(new HunterCandidate(
                        machineId,
                        new WorldPoint(snap.getSelfX(), snap.getSelfY(), snap.getSelfZ()),
                        cfg.maxHuntGoldCost));
            }
        }
        return out;
    }

    private Optional<Assignment> assignmentFor(ITeleportRouter localRouter, HunterCandidate hunter, WorldPoint target) {
        Optional<TeleportRoute> route = localRouter.route(hunter.position, target);
        if (!route.isPresent()) {
            return Optional.empty();
        }
        TeleportRoute r = route.get();
        if (r.getTotalCostGold() > hunter.maxGoldCost) {
            return Optional.empty();
        }
        return Optional.of(new Assignment(hunter.machineId, target, r.getEstimatedDurationMs()));
    }

    private HuntingRuntimeSettings readSettings(String groupName, String machineName) {
        ISettingsRegistry registry = settingsRegistry;
        if (registry == null) {
            return HuntingRuntimeSettings.disabled();
        }
        Map<String, Object> raw = registry.readRawSettings(groupName, machineName, "hunting");
        if (raw == null || raw.isEmpty()) {
            return HuntingRuntimeSettings.disabled();
        }
        SwarmRole role = parseRole(raw.get("role"));
        boolean enabled = boolValue(raw.get("huntDispatchEnabled"), true);
        long maxGoldCost = longValue(raw.get("maxHuntGoldCost"), 200_000L);
        return new HuntingRuntimeSettings(role, enabled, maxGoldCost);
    }

    private static boolean boolValue(Object raw, boolean fallback) {
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }
        if (raw instanceof String) {
            return Boolean.parseBoolean((String) raw);
        }
        return fallback;
    }

    private static long longValue(Object raw, long fallback) {
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        if (raw instanceof String) {
            try {
                return Long.parseLong((String) raw);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static SwarmRole parseRole(Object raw) {
        if (raw instanceof String) {
            try {
                return SwarmRole.valueOf(((String) raw).trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return SwarmRole.NONE;
            }
        }
        return SwarmRole.NONE;
    }

    private static final class Assignment {
        final String hunterMachineId;
        final WorldPoint targetPosition;
        final long etaMs;

        Assignment(String hunterMachineId, WorldPoint targetPosition, long etaMs) {
            this.hunterMachineId = Objects.requireNonNull(hunterMachineId);
            this.targetPosition = Objects.requireNonNull(targetPosition);
            this.etaMs = etaMs;
        }
    }

    private static final class HunterCandidate {
        final String machineId;
        final WorldPoint position;
        final long maxGoldCost;

        HunterCandidate(String machineId, WorldPoint position, long maxGoldCost) {
            this.machineId = machineId;
            this.position = position;
            this.maxGoldCost = maxGoldCost;
        }
    }

    private static final class ActiveHunt {
        final String huntId;
        final String hunterMachineId;
        final int targetRefId;

        ActiveHunt(String huntId, String hunterMachineId, int targetRefId) {
            this.huntId = huntId;
            this.hunterMachineId = hunterMachineId;
            this.targetRefId = targetRefId;
        }
    }

    private static final class HuntingRuntimeSettings {
        final SwarmRole role;
        final boolean enabled;
        final long maxHuntGoldCost;

        HuntingRuntimeSettings(SwarmRole role, boolean enabled, long maxHuntGoldCost) {
            this.role = role;
            this.enabled = enabled;
            this.maxHuntGoldCost = maxHuntGoldCost;
        }

        static HuntingRuntimeSettings disabled() {
            return new HuntingRuntimeSettings(SwarmRole.NONE, false, 0L);
        }
    }
}
