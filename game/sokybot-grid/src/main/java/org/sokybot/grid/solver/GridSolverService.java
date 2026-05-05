package org.sokybot.grid.solver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.sokybot.grid.api.IGridSolver;
import org.sokybot.grid.constraints.GridConstraintProvider;
import org.sokybot.grid.domain.GridBotEntity;
import org.sokybot.grid.domain.GridNode;
import org.sokybot.grid.domain.GridSolution;
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
 * OSGi-managed Timefold {@link SolverManager} with a bounded solve time for Hunting Grid formations.
 */
@Component(service = IGridSolver.class, immediate = true)
public final class GridSolverService implements IGridSolver {

    private static final Logger log = LoggerFactory.getLogger(GridSolverService.class);

    private volatile SolverManager<GridSolution, UUID> solverManager;

    @Activate
    void activate() {
        try {
            SolverConfig config = new SolverConfig();
            config.withSolutionClass(GridSolution.class);
            config.withEntityClasses(GridBotEntity.class);
            config.withConstraintProviderClass(GridConstraintProvider.class);
            config.withTerminationSpentLimit(Duration.ofSeconds(5));

            solverManager = SolverManager.create(config, new SolverManagerConfig());
            log.info("GridSolverService: SolverManager started (5s termination)");
        } catch (RuntimeException ex) {
            log.warn("GridSolverService: failed to start SolverManager: {}", ex.toString());
            solverManager = null;
        }
    }

    @Deactivate
    void deactivate() {
        if (solverManager != null) {
            solverManager.close();
            solverManager = null;
        }
    }

    @Override
    public Mono<GridSolution> calculateOptimalFormation(List<GridBotEntity> bots, List<GridNode> nodes) {
        SolverManager<GridSolution, UUID> mgr = solverManager;
        if (mgr == null) {
            return Mono.error(new IllegalStateException("GridSolverService: SolverManager not available"));
        }
        Objects.requireNonNull(bots, "bots");
        Objects.requireNonNull(nodes, "nodes");

        return Mono.fromCallable(() -> solveBlocking(mgr, bots, nodes)).subscribeOn(Schedulers.boundedElastic());
    }

    private static GridSolution solveBlocking(
            SolverManager<GridSolution, UUID> mgr,
            List<GridBotEntity> bots,
            List<GridNode> nodes) {
        GridSolution initialSolution = new GridSolution();
        initialSolution.setBotList(new ArrayList<>(bots));
        initialSolution.setNodeList(new ArrayList<>(nodes));
        initialSolution.setScore(HardSoftScore.ZERO);

        UUID problemId = UUID.randomUUID();
        SolverJob<GridSolution, UUID> solverJob = mgr.solve(problemId, initialSolution);
        try {
            return solverJob.getFinalBestSolution();
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Grid solving failed or was interrupted", e);
        }
    }
}
