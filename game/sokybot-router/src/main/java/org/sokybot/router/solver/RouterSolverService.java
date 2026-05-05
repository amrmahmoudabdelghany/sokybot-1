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
import org.sokybot.router.api.LogisticsBotDto;
import org.sokybot.router.api.LogisticsItemDto;
import org.sokybot.router.api.RouterPlan;
import org.sokybot.router.api.TradeMoveDto;
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
    public Mono<RouterPlan> calculateFieldLogistics(
            List<LogisticsBotDto> bots,
            List<LogisticsItemDto> items,
            RouterPlan previousPlan) {
        SolverManager<RouterSolution, UUID> mgr = solverManager;
        if (mgr == null) {
            return Mono.error(new IllegalStateException("RouterSolverService: SolverManager not available"));
        }
        Objects.requireNonNull(bots, "bots");
        Objects.requireNonNull(items, "items");

        return Mono.fromCallable(() -> solveBlocking(mgr, bots, items, previousPlan))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static RouterPlan solveBlocking(
            SolverManager<RouterSolution, UUID> mgr,
            List<LogisticsBotDto> bots,
            List<LogisticsItemDto> items,
            RouterPlan previousPlan) {
        RouterSolution initialSolution = new RouterSolution();
        initialSolution.setBotList(mapBotsFromDtos(bots));
        initialSolution.setItemList(mapItemsFromDtos(items));
        initialSolution.setBooleanList(Arrays.asList(Boolean.TRUE, Boolean.FALSE));
        initialSolution.setScore(HardSoftScore.ZERO);
        applyMuleWarmStart(initialSolution, previousPlan);

        UUID problemId = UUID.randomUUID();
        SolverJob<RouterSolution, UUID> solverJob = mgr.solve(problemId, initialSolution);
        RouterSolution solution;
        try {
            solution = solverJob.getFinalBestSolution();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Router solving failed or was interrupted", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new IllegalStateException("Router solving failed or was interrupted", cause);
        }
        return toRouterPlan(solution);
    }

    private static void applyMuleWarmStart(RouterSolution initialSolution, RouterPlan previousPlan) {
        if (previousPlan == null || previousPlan.getMuleMachineId() == null) {
            return;
        }
        String muleId = previousPlan.getMuleMachineId();
        List<SwarmBotEntity> botList = initialSolution.getBotList();
        if (botList == null) {
            return;
        }
        for (SwarmBotEntity bot : botList) {
            if (bot == null) {
                continue;
            }
            if (Objects.equals(muleId, bot.getMachineId())) {
                bot.setIsMule(Boolean.TRUE);
            } else {
                bot.setIsMule(Boolean.FALSE);
            }
        }
    }

    private static List<SwarmBotEntity> mapBotsFromDtos(List<LogisticsBotDto> dtos) {
        List<SwarmBotEntity> entities = new ArrayList<>();
        for (LogisticsBotDto dto : dtos) {
            if (dto == null) {
                continue;
            }
            SwarmBotEntity e = new SwarmBotEntity();
            e.setMachineId(dto.getMachineId());
            e.setMaxCapacity(dto.getMaxCapacity());
            e.setIsMule(null);
            entities.add(e);
        }
        return entities;
    }

    private static List<FieldItemEntity> mapItemsFromDtos(List<LogisticsItemDto> dtos) {
        List<FieldItemEntity> entities = new ArrayList<>();
        for (LogisticsItemDto dto : dtos) {
            if (dto == null) {
                continue;
            }
            FieldItemEntity e = new FieldItemEntity();
            e.setUniqueItemId(dto.getUniqueItemId());
            e.setSlotsTaken(dto.getSlotsTaken());
            e.setGoldValue(dto.getGoldValue());
            e.setOriginalBotMachineId(dto.getOriginalBotMachineId());
            e.setSlotIndex(dto.getSlotIndex());
            e.setStackQuantity(dto.getStackQuantity());
            e.setItemRefId(dto.getItemRefId());
            e.setAssignedBot(null);
            entities.add(e);
        }
        return entities;
    }

    private static RouterPlan toRouterPlan(RouterSolution solution) {
        if (solution == null) {
            return new RouterPlan(null, new ArrayList<>());
        }
        String muleMachineId = null;
        List<SwarmBotEntity> botList = solution.getBotList();
        if (botList != null) {
            for (SwarmBotEntity b : botList) {
                if (b != null && Boolean.TRUE.equals(b.getIsMule())) {
                    String id = b.getMachineId();
                    if (id != null && !id.isEmpty()) {
                        muleMachineId = id;
                    }
                    break;
                }
            }
        }

        List<TradeMoveDto> trades = new ArrayList<>();
        List<FieldItemEntity> itemList = solution.getItemList();
        if (itemList != null) {
            for (FieldItemEntity item : itemList) {
                if (item == null || item.getAssignedBot() == null) {
                    continue;
                }
                String from = item.getOriginalBotMachineId();
                SwarmBotEntity assigned = item.getAssignedBot();
                String to = assigned.getMachineId();
                if (from == null || to == null) {
                    continue;
                }
                if (from.equals(to)) {
                    continue;
                }
                trades.add(new TradeMoveDto(
                        from,
                        to,
                        item.getUniqueItemId(),
                        item.getSlotIndex(),
                        item.getStackQuantity(),
                        item.getItemRefId()));
            }
        }
        return new RouterPlan(muleMachineId, trades);
    }
}
