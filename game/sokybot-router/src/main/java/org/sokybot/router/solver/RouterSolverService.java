package org.sokybot.router.solver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.sokybot.router.api.IRouterSolver;
import org.sokybot.router.constraints.RouterConstraintProvider;
import org.sokybot.router.domain.FieldItemEntity;
import org.sokybot.router.domain.RouterSolution;
import org.sokybot.router.domain.SwarmBotEntity;
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
 * OSGi-managed Timefold {@link SolverManager} with a bounded solve time for Silk Road Router logistics.
 */
@Component(service = IRouterSolver.class, immediate = true)
public final class RouterSolverService implements IRouterSolver {

    private static final Logger log = LoggerFactory.getLogger(RouterSolverService.class);

    private volatile SolverManager<RouterSolution, UUID> solverManager;

    @Activate
    void activate() {
        try {
            SolverConfig config = new SolverConfig();
            config.withSolutionClass(RouterSolution.class);
            config.withEntityClasses(SwarmBotEntity.class, FieldItemEntity.class);
            config.withConstraintProviderClass(RouterConstraintProvider.class);
            config.withTerminationSpentLimit(Duration.ofSeconds(5));

            solverManager = SolverManager.create(config, new SolverManagerConfig());
            log.info("RouterSolverService: SolverManager started (5s termination)");
        } catch (RuntimeException ex) {
            log.warn("RouterSolverService: failed to start SolverManager: {}", ex.toString());
            solverManager = null;
        }
    }

    @Deactivate
    void deactivate() {
        SolverManager<RouterSolution, UUID> mgr = solverManager;
        solverManager = null;
        if (mgr != null) {
            try {
                mgr.close();
            } catch (RuntimeException ex) {
                log.debug("RouterSolverService close: {}", ex.toString());
            }
        }
    }

    @Override
    public Mono<RouterSolution> calculateFieldLogistics(List<SwarmBotEntity> bots, List<FieldItemEntity> items) {
        SolverManager<RouterSolution, UUID> mgr = solverManager;
        if (mgr == null) {
            return Mono.error(new IllegalStateException("RouterSolverService: SolverManager not available"));
        }
        Objects.requireNonNull(bots, "bots");
        Objects.requireNonNull(items, "items");

        return Mono.fromCallable(() -> solveBlocking(mgr, bots, items)).subscribeOn(Schedulers.boundedElastic());
    }

    private static RouterSolution solveBlocking(
            SolverManager<RouterSolution, UUID> mgr,
            List<SwarmBotEntity> bots,
            List<FieldItemEntity> items) {
        RouterSolution initialSolution = new RouterSolution();
        initialSolution.setBotList(new ArrayList<>(bots));
        initialSolution.setItemList(new ArrayList<>(items));
        initialSolution.setBooleanList(Arrays.asList(Boolean.TRUE, Boolean.FALSE));
        initialSolution.setScore(HardSoftScore.ZERO);

        UUID problemId = UUID.randomUUID();
        SolverJob<RouterSolution, UUID> solverJob = mgr.solve(problemId, initialSolution);
        try {
            return solverJob.getFinalBestSolution();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Router solving failed or was interrupted", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new IllegalStateException("Router solving failed or was interrupted", cause);
        }
    }
}
