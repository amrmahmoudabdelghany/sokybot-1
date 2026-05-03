package org.sokybot.behaviors.swarm.roster;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.sokybot.engine.IEngine;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.party.api.IPartyDirectory;
import org.sokybot.party.api.IPartyModel;
import org.sokybot.party.api.IPartySnapshot;
import org.sokybot.party.api.PartyMatrixSettings;
import org.sokybot.party.api.PartyMember;
import org.sokybot.party.coordination.api.IPartyCoordinator;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.settings.fleet.FleetMachineSettings;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.RosterBlackboardKeys;
import org.sokybot.swarm.api.SwarmDispatchEvent;
import org.sokybot.swarm.api.SwarmRecruitmentBidEvent;
import org.sokybot.swarm.api.SwarmRecruitmentEvent;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.projections.api.ITownModel;

import reactor.core.Disposable;
import reactor.core.Disposables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors party roster vs {@link PartyMatrixSettings}, publishes recruitment + dispatch (Epic #17 Phase 2).
 */
@Component(service = RosterSupervisorComponent.class, immediate = true)
public final class RosterSupervisorComponent {

    private static final Logger log = LoggerFactory.getLogger(RosterSupervisorComponent.class);

    private static final String PARTY_SCOPE = "party";

    private static final String PARTY_MATRIX_SCOPE = "party-matrix";

    @Reference
    private ISwarmEventBus swarmBus;

    @Reference
    private IPartyModel partyModel;

    @Reference
    private ISettingsRegistry settingsRegistry;

    @Reference
    private ISokybotContext sokybotContext;

    @Reference
    private IPartyDirectory partyDirectory;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITownModel townModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IPartyCoordinator partyCoordinator;

    private final Disposable.Composite subscriptions = Disposables.composite();

    private final ConcurrentHashMap<String, Long> gapStartByLeaderProfile = new ConcurrentHashMap<>();

    private final Set<String> pendingRecruitment = ConcurrentHashMap.newKeySet();

    @Activate
    void activate() {
        ISokybotContext ctx = sokybotContext;
        IPartyModel parties = partyModel;
        if (ctx == null || parties == null) {
            log.warn("RosterSupervisor: missing ISokybotContext or IPartyModel");
            return;
        }
        for (IMachineContext machine : allMachines(ctx)) {
            if (machine == null) {
                continue;
            }
            String leaderFullName = machine.fullName();
            PartyMatrixSettings matrix = readMatrixSettings(leaderFullName);
            if (matrix == null || !matrix.isRosterMatrixEnabled() || matrix.getProfileIdToRequiredCount().isEmpty()) {
                continue;
            }
            if (!isLeaderCandidate(leaderFullName)) {
                continue;
            }
            final String leader = leaderFullName;
            Disposable d = parties.observe(leader)
                    .onErrorContinue((err, trigger) -> log.warn(
                            "RosterSupervisor party observe [{}]: {}",
                            leader,
                            err != null ? err.getMessage() : "unknown"))
                    .subscribe(snap -> {
                        if (snap != null) {
                            onLeaderPartySnapshot(leader, snap);
                        }
                    });
            subscriptions.add(d);
            log.info("RosterSupervisor: supervising leader {}", leader);
        }
    }

    @Deactivate
    void deactivate() {
        subscriptions.dispose();
        gapStartByLeaderProfile.clear();
        pendingRecruitment.clear();
    }

    private void onLeaderPartySnapshot(String leaderFullName, IPartySnapshot snap) {
        PartyMatrixSettings matrix = readMatrixSettings(leaderFullName);
        if (matrix == null || !matrix.isRosterMatrixEnabled() || matrix.getProfileIdToRequiredCount().isEmpty()) {
            return;
        }

        Map<String, Integer> current = tallyProfiles(snap);
        Map<String, Integer> required = matrix.getProfileIdToRequiredCount();
        long now = System.currentTimeMillis();
        long debounceMs = matrix.getGapDebounceMs();

        for (Map.Entry<String, Integer> e : required.entrySet()) {
            String profileId = e.getKey();
            if (profileId == null || profileId.trim().isEmpty()) {
                continue;
            }
            String pid = profileId.trim();
            int req = e.getValue() == null ? 0 : Math.max(0, e.getValue().intValue());
            int cur = current.getOrDefault(pid, 0);
            String pendingKey = pendingKey(leaderFullName, pid);

            if (req <= cur) {
                gapStartByLeaderProfile.remove(pendingKey);
                continue;
            }

            if (pendingRecruitment.contains(pendingKey)) {
                continue;
            }

            long start = gapStartByLeaderProfile.computeIfAbsent(pendingKey, k -> Long.valueOf(now)).longValue();
            if (now - start < debounceMs) {
                continue;
            }

            gapStartByLeaderProfile.remove(pendingKey);
            pendingRecruitment.add(pendingKey);
            try {
                beginRecruitment(leaderFullName, pid, matrix);
            } catch (Exception ex) {
                pendingRecruitment.remove(pendingKey);
                log.warn("RosterSupervisor recruitment failed leader={} profile={}: {}", leaderFullName, pid,
                        ex.getMessage());
            }
        }
    }

    private void beginRecruitment(String leaderFullName, String missingProfileId, PartyMatrixSettings matrix) {
        ISwarmEventBus bus = swarmBus;
        if (bus == null) {
            pendingRecruitment.remove(pendingKey(leaderFullName, missingProfileId));
            return;
        }

        WorldPoint rallyPoint = resolveRallyPoint(leaderFullName);
        long now = System.currentTimeMillis();
        String recruitmentId = UUID.randomUUID().toString();
        long bidWindowMs = Math.max(1L, matrix.getRecruitmentBidWindowMs());
        long bidDeadlineEpochMs = now + bidWindowMs;
        String pendingKey = pendingKey(leaderFullName, missingProfileId);

        Disposable bidDisposable = bus.observe(SwarmRecruitmentBidEvent.class)
                .filter(bid -> recruitmentId.equals(bid.getRecruitmentId()))
                .take(Duration.ofMillis(bidWindowMs))
                .collectList()
                .doFinally(sig -> pendingRecruitment.remove(pendingKey))
                .subscribe(
                        bids -> dispatchWinnerIfAny(leaderFullName, recruitmentId, rallyPoint, bids),
                        err -> log.warn("RosterSupervisor bid window [{}]: {}", recruitmentId,
                                err != null ? err.getMessage() : "unknown"));
        subscriptions.add(bidDisposable);

        bus.publish(new SwarmRecruitmentEvent(
                leaderFullName,
                now,
                recruitmentId,
                missingProfileId,
                rallyPoint,
                bidDeadlineEpochMs));
        log.info(
                "RosterSupervisor published recruitmentId={} leader={} missingProfile={}",
                recruitmentId,
                leaderFullName,
                missingProfileId);
    }

    private void dispatchWinnerIfAny(
            String leaderFullName,
            String recruitmentId,
            WorldPoint rallyPoint,
            List<SwarmRecruitmentBidEvent> bids) {
        if (bids == null || bids.isEmpty()) {
            log.debug("RosterSupervisor no bids for recruitmentId={} leader={}", recruitmentId, leaderFullName);
            return;
        }
        bids.sort(Comparator.comparing(SwarmRecruitmentBidEvent::getBidderMachineId,
                Comparator.nullsLast(String::compareTo)));
        SwarmRecruitmentBidEvent winner = bids.get(0);
        ISwarmEventBus bus = swarmBus;
        if (bus == null) {
            return;
        }
        String dispatchId = UUID.randomUUID().toString();
        long ts = System.currentTimeMillis();
        bus.publish(new SwarmDispatchEvent(
                winner.getBidderMachineId(),
                ts,
                dispatchId,
                rallyPoint,
                recruitmentId,
                "ROSTER_RALLY"));
        log.info(
                "RosterSupervisor dispatch winner={} leader={} recruitmentId={}",
                winner.getBidderMachineId(),
                leaderFullName,
                recruitmentId);
        leaderHandoffPendingInvite(leaderFullName, winner);
    }

    private void leaderHandoffPendingInvite(String leaderFullName, SwarmRecruitmentBidEvent winner) {
        String charName = winner.getBidderCharName();
        if (charName == null || charName.trim().isEmpty()) {
            return;
        }
        ISokybotContext ctx = sokybotContext;
        if (ctx == null) {
            return;
        }
        IMachineContext leaderMachine = findMachineByFullName(ctx, leaderFullName);
        if (leaderMachine == null || !leaderMachine.isRunning()) {
            return;
        }
        IEngine engine = leaderMachine.getEngine();
        if (engine == null || !engine.isRunning()) {
            return;
        }
        try {
            Optional<IWorkflowContext> wfOpt = engine.optionalWorkflowContext();
            if (!wfOpt.isPresent()) {
                return;
            }
            Map<String, Object> pd = wfOpt.get().getPersistentData();
            @SuppressWarnings("unchecked")
            Set<String> pending = (Set<String>) pd.get(RosterBlackboardKeys.KEY_PENDING_ROSTER_INVITES);
            Set<String> use = pending;
            if (use == null) {
                use = ConcurrentHashMap.newKeySet();
                pd.put(RosterBlackboardKeys.KEY_PENDING_ROSTER_INVITES, use);
            }
            use.add(charName.trim());
        } catch (Exception e) {
            log.warn("RosterSupervisor leader handoff failed leader={}: {}", leaderFullName, e.getMessage());
        }
        IPartyCoordinator coord = partyCoordinator;
        if (coord != null) {
            try {
                coord.requestInvite(leaderFullName, winner.getBidderMachineId());
            } catch (Exception ex) {
                log.warn("RosterSupervisor requestInvite failed: {}", ex.getMessage());
            }
        }
    }

    private static IMachineContext findMachineByFullName(ISokybotContext ctx, String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return null;
        }
        String key = fullName.trim();
        for (IGroupContext g : ctx.getGroups()) {
            if (g == null) {
                continue;
            }
            for (IMachineContext m : g.getMachines()) {
                if (m != null && key.equals(m.fullName())) {
                    return m;
                }
            }
        }
        return null;
    }

    private WorldPoint resolveRallyPoint(String leaderFullName) {
        ITownModel tm = townModel;
        if (tm == null) {
            return new WorldPoint(0, 0, 0);
        }
        try {
            Optional<ITownSnapshot> opt = tm.snapshot(leaderFullName);
            if (!opt.isPresent()) {
                return new WorldPoint(0, 0, 0);
            }
            ITownSnapshot ts = opt.get();
            return new WorldPoint((int) ts.getSelfX(), (int) ts.getSelfY(), (int) ts.getSelfZ());
        } catch (Exception e) {
            log.debug("RosterSupervisor rally point fallback for {}: {}", leaderFullName, e.getMessage());
            return new WorldPoint(0, 0, 0);
        }
    }

    private Map<String, Integer> tallyProfiles(IPartySnapshot snap) {
        Map<String, Integer> counts = new HashMap<>();
        if (snap == null || snap.getMembers() == null) {
            return counts;
        }
        ISettingsRegistry reg = settingsRegistry;
        if (reg == null) {
            return counts;
        }
        for (PartyMember m : snap.getMembers()) {
            if (m == null) {
                continue;
            }
            String charName = m.getCharName();
            if (charName == null || charName.trim().isEmpty()) {
                continue;
            }
            try {
                Optional<String> machineOpt = partyDirectory.resolveMachineForCharacterName(charName.trim());
                if (!machineOpt.isPresent()) {
                    continue;
                }
                String machineFullName = machineOpt.get().trim();
                String[] gm = splitMachineFullName(machineFullName);
                if (gm == null) {
                    continue;
                }
                ISettingsProvider<FleetMachineSettings> provider = reg.getProvider(
                        gm[0],
                        gm[1],
                        FleetMachineSettings.SCOPE_NAME,
                        FleetMachineSettings.class);
                if (provider == null) {
                    continue;
                }
                FleetMachineSettings fleet = provider.get();
                if (fleet == null) {
                    continue;
                }
                String profileId = fleet.getProfileId();
                if (profileId == null || profileId.trim().isEmpty()) {
                    continue;
                }
                String pid = profileId.trim();
                counts.merge(pid, 1, Integer::sum);
            } catch (Exception ex) {
                log.trace("RosterSupervisor tally skip member {}: {}", charName, ex.getMessage());
            }
        }
        return counts;
    }

    private PartyMatrixSettings readMatrixSettings(String machineFullName) {
        String[] gm = splitMachineFullName(machineFullName);
        if (gm == null) {
            return null;
        }
        try {
            ISettingsProvider<PartyMatrixSettings> provider = settingsRegistry.getProvider(
                    gm[0],
                    gm[1],
                    PARTY_MATRIX_SCOPE,
                    PartyMatrixSettings.class);
            return provider != null ? provider.get() : null;
        } catch (Exception e) {
            log.trace("RosterSupervisor read matrix {}: {}", machineFullName, e.getMessage());
            return null;
        }
    }

    private boolean isLeaderCandidate(String machineFullName) {
        String[] gm = splitMachineFullName(machineFullName);
        if (gm == null) {
            return false;
        }
        try {
            Map<String, Object> raw = settingsRegistry.readRawSettings(gm[0], gm[1], PARTY_SCOPE);
            if (raw == null) {
                return true;
            }
            Object v = raw.get("leaderMachineFullName");
            if (!(v instanceof String)) {
                return true;
            }
            String configured = ((String) v).trim();
            return configured.isEmpty() || configured.equals(machineFullName);
        } catch (Exception e) {
            log.trace("RosterSupervisor leader check {}: {}", machineFullName, e.getMessage());
            return false;
        }
    }

    private static List<IMachineContext> allMachines(ISokybotContext ctx) {
        List<IMachineContext> out = new ArrayList<>();
        for (IGroupContext g : ctx.getGroups()) {
            if (g == null) {
                continue;
            }
            for (IMachineContext m : g.getMachines()) {
                if (m != null) {
                    out.add(m);
                }
            }
        }
        return out;
    }

    private static String pendingKey(String leaderFullName, String profileId) {
        return leaderFullName + "|" + profileId;
    }

    private static String[] splitMachineFullName(String machineFullName) {
        if (machineFullName == null) {
            return null;
        }
        int dot = machineFullName.indexOf('.');
        if (dot <= 0 || dot >= machineFullName.length() - 1) {
            return null;
        }
        return new String[] { machineFullName.substring(0, dot), machineFullName.substring(dot + 1) };
    }
}
