package org.sokybot.warroom.solver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.sokybot.warroom.api.IWarRoomSolver;
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
    public Mono<PartyRosterSolution> calculateOptimalRoster(List<SwarmBotEntity> availableBots, int numberOfParties) {
        SolverManager<PartyRosterSolution, UUID> mgr = solverManager;
        if (mgr == null) {
            return Mono.error(new IllegalStateException("WarRoomSolverService: SolverManager not available"));
        }
        if (numberOfParties < 1) {
            return Mono.error(new IllegalArgumentException("numberOfParties must be >= 1"));
        }
        Objects.requireNonNull(availableBots, "availableBots");

        return Mono.fromCallable(() -> solveBlocking(mgr, availableBots, numberOfParties))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static PartyRosterSolution solveBlocking(
            SolverManager<PartyRosterSolution, UUID> mgr,
            List<SwarmBotEntity> availableBots,
            int numberOfParties) {
        PartyRosterSolution initialSolution = buildInitialSolution(availableBots, numberOfParties);
        UUID problemId = UUID.randomUUID();
        SolverJob<PartyRosterSolution, UUID> solverJob = mgr.solve(problemId, initialSolution);
        try {
            return solverJob.getFinalBestSolution();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("War Room solve interrupted", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new IllegalStateException("War Room solve failed", cause);
        }
    }

    private static PartyRosterSolution buildInitialSolution(List<SwarmBotEntity> availableBots, int numberOfParties) {
        List<WarRoomParty> parties = new ArrayList<>(numberOfParties);
        for (int i = 1; i <= numberOfParties; i++) {
            parties.add(new WarRoomParty("party_" + i, 8));
        }
        PartyRosterSolution initialSolution = new PartyRosterSolution();
        initialSolution.setPartyList(parties);
        initialSolution.setBotList(new ArrayList<>(availableBots));
        initialSolution.setScore(HardSoftScore.ZERO);
        return initialSolution;
    }
}
