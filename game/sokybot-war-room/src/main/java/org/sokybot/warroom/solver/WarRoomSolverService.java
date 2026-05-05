package org.sokybot.warroom.solver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.sokybot.warroom.api.IWarRoomSolver;
import org.sokybot.warroom.api.SwarmBotDto;
import org.sokybot.warroom.api.WarRoomPlan;
import org.sokybot.warroom.constraints.PartyRosterConstraintProvider;
import org.sokybot.warroom.domain.PartyRosterSolution;
import org.sokybot.warroom.domain.SwarmBotEntity;
import org.sokybot.warroom.domain.WarRoomParty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * OSGi-managed Timefold {@link SolverManager} with a bounded solve time for live swarm use.
 */
@Component(service = IWarRoomSolver.class, immediate = true)
public final class WarRoomSolverService implements IWarRoomSolver {

    private static final Logger log = LoggerFactory.getLogger(WarRoomSolverService.class);

    private volatile SolverManager<PartyRosterSolution, UUID> solverManager;

    @Activate
    void activate() {
        try {
            SolverConfig config = new SolverConfig();
            config.withSolutionClass(PartyRosterSolution.class);
            config.withEntityClasses(SwarmBotEntity.class);
            config.withConstraintProviderClass(PartyRosterConstraintProvider.class);
            config.withTerminationSpentLimit(Duration.ofSeconds(5));

            solverManager = SolverManager.create(config, new SolverManagerConfig());
            log.info("WarRoomSolverService: SolverManager started (5s termination)");
        } catch (RuntimeException ex) {
            log.warn("WarRoomSolverService: failed to start SolverManager: {}", ex.toString());
            solverManager = null;
        }
    }

    @Deactivate
    void deactivate() {
        SolverManager<PartyRosterSolution, UUID> mgr = solverManager;
        solverManager = null;
        if (mgr != null) {
            try {
                mgr.close();
            } catch (RuntimeException ex) {
                log.debug("WarRoomSolverService close: {}", ex.toString());
            }
        }
    }

    @Override
    public Mono<WarRoomPlan> calculateOptimalRoster(
            List<SwarmBotDto> availableBots,
            int numberOfParties,
            WarRoomPlan previousPlan) {
        SolverManager<PartyRosterSolution, UUID> mgr = solverManager;
        if (mgr == null) {
            return Mono.error(new IllegalStateException("WarRoomSolverService: SolverManager not available"));
        }
        if (numberOfParties < 1) {
            return Mono.error(new IllegalArgumentException("numberOfParties must be >= 1"));
        }
        Objects.requireNonNull(availableBots, "availableBots");

        return Mono.fromCallable(() -> solveBlocking(mgr, availableBots, numberOfParties, previousPlan))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static WarRoomPlan solveBlocking(
            SolverManager<PartyRosterSolution, UUID> mgr,
            List<SwarmBotDto> availableBots,
            int numberOfParties,
            WarRoomPlan previousPlan) {
        PartyRosterSolution initialSolution = buildInitialSolution(availableBots, numberOfParties);
        applyWarmStart(initialSolution, previousPlan);
        UUID problemId = UUID.randomUUID();
        SolverJob<PartyRosterSolution, UUID> solverJob = mgr.solve(problemId, initialSolution);
        PartyRosterSolution solution;
        try {
            solution = solverJob.getFinalBestSolution();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("War Room solve interrupted", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new IllegalStateException("War Room solve failed", cause);
        }
        return toWarRoomPlan(solution);
    }

    private static WarRoomPlan toWarRoomPlan(PartyRosterSolution solution) {
        if (solution == null) {
            return new WarRoomPlan(new HashMap<>());
        }
        List<WarRoomParty> parties = solution.getPartyList();
        List<SwarmBotEntity> bots = solution.getBotList();
        if (parties == null || bots == null) {
            return new WarRoomPlan(new HashMap<>());
        }

        Map<String, List<String>> assignments = new HashMap<>();
        for (WarRoomParty party : parties) {
            if (party == null) {
                continue;
            }
            String partyId = party.getId();
            List<SwarmBotEntity> inParty = new ArrayList<>();
            for (SwarmBotEntity bot : bots) {
                if (bot == null) {
                    continue;
                }
                WarRoomParty assigned = bot.getAssignedParty();
                if (assigned != null && Objects.equals(partyId, assigned.getId())) {
                    inParty.add(bot);
                }
            }
            if (inParty.isEmpty()) {
                continue;
            }
            SwarmBotEntity leader = inParty.stream()
                    .max(Comparator.comparingInt(SwarmBotEntity::getLevel)
                            .thenComparing(SwarmBotEntity::getMachineId))
                    .orElse(null);
            if (leader == null) {
                continue;
            }
            List<String> memberIds = new ArrayList<>();
            for (SwarmBotEntity b : inParty) {
                memberIds.add(b.getMachineId());
            }
            assignments.put(leader.getMachineId(), memberIds);
        }
        return new WarRoomPlan(assignments);
    }

    private static PartyRosterSolution buildInitialSolution(List<SwarmBotDto> availableBots, int numberOfParties) {
        List<WarRoomParty> parties = new ArrayList<>(numberOfParties);
        for (int i = 1; i <= numberOfParties; i++) {
            parties.add(new WarRoomParty("party_" + i, 8));
        }
        PartyRosterSolution initialSolution = new PartyRosterSolution();
        initialSolution.setPartyList(parties);
        initialSolution.setBotList(mapDtosToEntities(availableBots));
        initialSolution.setScore(HardSoftScore.ZERO);
        return initialSolution;
    }

    /**
     * Assigns parties from a persisted plan so Timefold sees a feasible partial solution (warm start).
     */
    private static void applyWarmStart(PartyRosterSolution initialSolution, WarRoomPlan previousPlan) {
        if (previousPlan == null) {
            return;
        }
        Map<String, List<String>> assignments = previousPlan.getPartyAssignments();
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        List<WarRoomParty> parties = initialSolution.getPartyList();
        List<SwarmBotEntity> bots = initialSolution.getBotList();
        if (parties == null || parties.isEmpty() || bots == null || bots.isEmpty()) {
            return;
        }
        Map<String, SwarmBotEntity> botByMachineId = new HashMap<>();
        for (SwarmBotEntity bot : bots) {
            if (bot != null && bot.getMachineId() != null) {
                botByMachineId.put(bot.getMachineId(), bot);
            }
        }
        int partyIndex = 0;
        for (Map.Entry<String, List<String>> entry : assignments.entrySet()) {
            if (partyIndex >= parties.size()) {
                break;
            }
            WarRoomParty party = parties.get(partyIndex);
            List<String> memberIds = entry.getValue();
            if (memberIds != null) {
                for (String memberId : memberIds) {
                    if (memberId == null) {
                        continue;
                    }
                    SwarmBotEntity entity = botByMachineId.get(memberId);
                    if (entity != null) {
                        entity.setAssignedParty(party);
                    }
                }
            }
            partyIndex++;
        }
    }

    private static List<SwarmBotEntity> mapDtosToEntities(List<SwarmBotDto> dtos) {
        List<SwarmBotEntity> entities = new ArrayList<>();
        for (SwarmBotDto dto : dtos) {
            if (dto == null) {
                continue;
            }
            SwarmBotEntity e = new SwarmBotEntity();
            e.setMachineId(dto.getMachineId());
            e.setLevel(dto.getLevel());
            e.setRole(dto.getRole());
            e.setDpsScore(dto.getDpsScore());
            e.setAssignedParty(null);
            entities.add(e);
        }
        return entities;
    }
}
