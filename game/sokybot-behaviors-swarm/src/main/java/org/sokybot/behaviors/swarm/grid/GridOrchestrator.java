package org.sokybot.behaviors.swarm.grid;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.engine.IEngine;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.enums.JobType;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.grid.api.IGridSolver;
import org.sokybot.grid.domain.GridBotEntity;
import org.sokybot.grid.domain.GridNode;
import org.sokybot.grid.domain.GridSolution;
import org.sokybot.party.api.shapeshifter.SwarmTacticalRole;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.warroom.SwarmFormationCommandEvent;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.projections.api.ITownModel;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Epic #25 Phase 4: periodic grid solve around a leader and publishes formation hold commands.
 */
@Component(immediate = true)
public final class GridOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(GridOrchestrator.class);

    private static final String REQUESTER = "hunting-grid";

    private static final double NODE_RADIUS = 15.0;

    private static final int NODE_COUNT = 30;

    @Reference
    private ISwarmEventBus swarmEventBus;

    @Reference
    private ISokybotContext sokybotContext;

    @Reference
    private IGridSolver gridSolver;

    @Reference
    private ITownModel townModel;

    private volatile Disposable timerDisposable;

    @Activate
    void activate() {
        timerDisposable = Flux.interval(Duration.ofSeconds(60)).subscribe(this::calculateGrid);
    }

    @Deactivate
    void deactivate() {
        Disposable d = timerDisposable;
        timerDisposable = null;
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private void calculateGrid(Long tick) {
        try {
            ISokybotContext ctx = sokybotContext;
            IGridSolver solver = gridSolver;
            ISwarmEventBus bus = swarmEventBus;
            ITownModel town = townModel;
            if (ctx == null || solver == null || bus == null || town == null) {
                return;
            }

            try {
                for (IGroupContext g : ctx.getGroups()) {
                    if (g == null) {
                        continue;
                    }
                    List<IMachineContext> runningMachines = new ArrayList<>();
                    try {
                        for (IMachineContext m : g.getMachines()) {
                            if (m != null && m.isRunning()) {
                                runningMachines.add(m);
                            }
                        }
                    } catch (Exception ex) {
                        log.warn("GridOrchestrator: enumerate machines in group {}: {}", g.name(), ex.getMessage());
                        continue;
                    }

                    if (runningMachines.size() < 2) {
                        continue;
                    }

                    IMachineContext leader = pickLeader(runningMachines);
                    if (leader == null) {
                        continue;
                    }

                    double leaderX;
                    double leaderY;
                    try {
                        double[] xy = resolveLeaderXY(leader, town);
                        leaderX = xy[0];
                        leaderY = xy[1];
                    } catch (Exception ex) {
                        log.warn("GridOrchestrator: leader position unavailable (group {}): {}", g.name(), ex.getMessage());
                        continue;
                    }

                    String groupKey = g.name() != null ? g.name().trim() : "group";
                    List<GridNode> nodes = new ArrayList<>(NODE_COUNT);
                    for (int i = 0; i < NODE_COUNT; i++) {
                        double angle = i * (Math.PI * 2) / NODE_COUNT;
                        double x = leaderX + NODE_RADIUS * Math.cos(angle);
                        double y = leaderY + NODE_RADIUS * Math.sin(angle);
                        GridNode node = new GridNode();
                        node.setNodeId(groupKey + "-grid-node-" + i);
                        node.setX(x);
                        node.setY(y);
                        nodes.add(node);
                    }

                    String leaderId = leader.fullName();
                    List<GridBotEntity> bots = new ArrayList<>();
                    for (IMachineContext machine : runningMachines) {
                        try {
                            GridBotEntity bot = mapMachineToEntity(machine, Objects.equals(machine.fullName(), leaderId));
                            if (bot != null) {
                                bots.add(bot);
                            }
                        } catch (Exception ex) {
                            log.warn(
                                    "GridOrchestrator: skip machine {}: {}",
                                    machine != null ? machine.fullName() : "?",
                                    ex.getMessage());
                        }
                    }

                    if (bots.isEmpty()) {
                        continue;
                    }

                    final long tickVal = tick != null ? tick.longValue() : -1L;
                    solver.calculateOptimalFormation(bots, nodes)
                            .subscribe(
                                    solution -> {
                                        try {
                                            publishFormation(bus, solution, tickVal);
                                        } catch (Exception ex) {
                                            log.warn("GridOrchestrator: publish failed: {}", ex.getMessage());
                                        }
                                    },
                                    err -> log.error("GridOrchestrator: solver failed: {}", err.toString()));
                }
            } catch (Exception ex) {
                log.warn("GridOrchestrator: group iteration failed: {}", ex.getMessage());
                return;
            }
        } catch (Exception ex) {
            log.warn("GridOrchestrator: calculateGrid failed: {}", ex.toString());
        }
    }

    private static IMachineContext pickLeader(List<IMachineContext> running) {
        for (IMachineContext m : running) {
            SwarmTacticalRole role = mapTacticalRole(m);
            if (role == SwarmTacticalRole.HEALER || role == SwarmTacticalRole.BUFFER) {
                return m;
            }
        }
        return running.get(0);
    }

    private static SwarmTacticalRole mapTacticalRole(IMachineContext machine) {
        try {
            IEngine engine = machine.getEngine();
            if (engine != null) {
                Optional<IWorkflowContext> wctx = engine.optionalWorkflowContext();
                if (wctx.isPresent()) {
                    IGameModel gm = wctx.get().getGameModel();
                    if (gm != null && gm.getTrainer() != null) {
                        String job = gm.getTrainer().getJobName();
                        if (job != null) {
                            String u = job.toUpperCase(Locale.ROOT);
                            if (u.contains("CLERIC")) {
                                return SwarmTacticalRole.HEALER;
                            }
                            if (u.contains("BARD") || u.contains("FORCE")) {
                                return SwarmTacticalRole.BUFFER;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            return SwarmTacticalRole.DPS;
        }
        return SwarmTacticalRole.DPS;
    }

    private static double[] resolveLeaderXY(IMachineContext leader, ITownModel townModel) {
        String fullName = leader.fullName();
        try {
            Optional<ITownSnapshot> snap = townModel.snapshot(fullName);
            if (snap.isPresent()) {
                ITownSnapshot s = snap.get();
                return new double[] { s.getSelfX(), s.getSelfY() };
            }
        } catch (Exception ex) {
            log.debug("GridOrchestrator: town snapshot failed for {}: {}", fullName, ex.getMessage());
        }

        try {
            IEngine engine = leader.getEngine();
            if (engine != null) {
                Optional<IWorkflowContext> wctx = engine.optionalWorkflowContext();
                if (wctx.isPresent()) {
                    IGameModel gm = wctx.get().getGameModel();
                    if (gm != null && gm.getTrainer() != null && gm.getTrainer().getPosition() != null) {
                        return new double[] {
                            gm.getTrainer().getPosition().getX(),
                            gm.getTrainer().getPosition().getY()
                        };
                    }
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("engine position", ex);
        }

        throw new IllegalStateException("no leader coordinates");
    }

    private static GridBotEntity mapMachineToEntity(IMachineContext machine, boolean isLeaderBufferSlot) {
        String machineId = machine.fullName();
        double attackRange = 15.0;
        try {
            IEngine engine = machine.getEngine();
            if (engine != null) {
                Optional<IWorkflowContext> wctx = engine.optionalWorkflowContext();
                if (wctx.isPresent()) {
                    IGameModel gm = wctx.get().getGameModel();
                    if (gm != null && gm.getTrainer() != null) {
                        JobType jt = gm.getTrainer().getJobType();
                        if (jt == JobType.Hunter) {
                            attackRange = 30.0;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            attackRange = 15.0;
        }

        GridBotEntity bot = new GridBotEntity();
        bot.setMachineId(machineId);
        bot.setAttackRange(attackRange);
        bot.setBuffer(isLeaderBufferSlot);
        bot.setAssignedNode(null);
        return bot;
    }

    private void publishFormation(ISwarmEventBus bus, GridSolution solution, long tick) {
        if (solution == null || bus == null) {
            return;
        }
        List<GridBotEntity> bots = solution.getBotList();
        if (bots == null) {
            return;
        }
        long ts = System.currentTimeMillis();
        String formationSuffix = "grid-" + tick;
        for (GridBotEntity bot : bots) {
            if (bot == null) {
                continue;
            }
            GridNode node = bot.getAssignedNode();
            if (node == null) {
                continue;
            }
            String reqId = UUID.randomUUID().toString();
            SwarmFormationCommandEvent cmd = new SwarmFormationCommandEvent(
                    REQUESTER,
                    ts,
                    reqId,
                    bot.getMachineId(),
                    node.getX(),
                    node.getY(),
                    formationSuffix);
            try {
                bus.publish(cmd);
            } catch (Exception ex) {
                log.warn(
                        "GridOrchestrator: publish SwarmFormationCommandEvent failed for {}: {}",
                        bot.getMachineId(),
                        ex.getMessage());
            }
        }
    }
}
