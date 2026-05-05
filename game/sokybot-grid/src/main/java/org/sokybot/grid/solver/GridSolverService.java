package org.sokybot.grid.solver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.sokybot.grid.api.GridBotDto;
import org.sokybot.grid.api.GridNodeDto;
import org.sokybot.grid.api.GridPlan;
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
        SolverManager<GridSolution, UUID> mgr = solverManager;
        solverManager = null;
        if (mgr != null) {
            try {
                mgr.close();
            } catch (RuntimeException ex) {
                log.debug("Error closing solver", ex);
            }
        }
    }

    @Override
    public Mono<GridPlan> calculateOptimalFormation(List<GridBotDto> bots, List<GridNodeDto> nodes) {
        SolverManager<GridSolution, UUID> mgr = solverManager;
        if (mgr == null) {
            return Mono.error(new IllegalStateException("GridSolverService: SolverManager not available"));
        }
        Objects.requireNonNull(bots, "bots");
        Objects.requireNonNull(nodes, "nodes");

        return Mono.fromCallable(() -> solveBlocking(mgr, bots, nodes)).subscribeOn(Schedulers.boundedElastic());
    }

    private static GridPlan solveBlocking(
            SolverManager<GridSolution, UUID> mgr,
            List<GridBotDto> bots,
            List<GridNodeDto> nodes) {
        GridSolution initialSolution = new GridSolution();
        initialSolution.setBotList(mapBotsFromDtos(bots));
        initialSolution.setNodeList(mapNodesFromDtos(nodes));
        initialSolution.setScore(HardSoftScore.ZERO);

        UUID problemId = UUID.randomUUID();
        SolverJob<GridSolution, UUID> solverJob = mgr.solve(problemId, initialSolution);
        GridSolution solution;
        try {
            solution = solverJob.getFinalBestSolution();
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Grid solving failed or was interrupted", e);
        }
        return toGridPlan(solution);
    }

    private static List<GridBotEntity> mapBotsFromDtos(List<GridBotDto> dtos) {
        List<GridBotEntity> entities = new ArrayList<>();
        for (GridBotDto dto : dtos) {
            if (dto == null) {
                continue;
            }
            GridBotEntity e = new GridBotEntity();
            e.setMachineId(dto.getMachineId());
            e.setAttackRange(dto.getAttackRange());
            e.setBuffer(dto.isBuffer());
            e.setAssignedNode(null);
            entities.add(e);
        }
        return entities;
    }

    private static List<GridNode> mapNodesFromDtos(List<GridNodeDto> dtos) {
        List<GridNode> nodes = new ArrayList<>();
        for (GridNodeDto dto : dtos) {
            if (dto == null) {
                continue;
            }
            GridNode n = new GridNode();
            n.setNodeId(dto.getNodeId());
            n.setX(dto.getX());
            n.setY(dto.getY());
            nodes.add(n);
        }
        return nodes;
    }

    private static GridPlan toGridPlan(GridSolution solution) {
        Map<String, GridNodeDto> assignments = new LinkedHashMap<>();
        if (solution == null || solution.getBotList() == null) {
            return new GridPlan(assignments);
        }
        for (GridBotEntity bot : solution.getBotList()) {
            if (bot == null || bot.getAssignedNode() == null) {
                continue;
            }
            GridNode n = bot.getAssignedNode();
            assignments.put(
                    bot.getMachineId(),
                    new GridNodeDto(n.getNodeId(), n.getX(), n.getY()));
        }
        return new GridPlan(assignments);
    }
}
