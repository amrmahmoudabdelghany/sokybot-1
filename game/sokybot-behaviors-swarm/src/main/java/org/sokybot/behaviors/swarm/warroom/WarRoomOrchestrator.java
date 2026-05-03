package org.sokybot.behaviors.swarm.warroom;

import java.util.ArrayList;
import java.util.Comparator;
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
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.party.api.shapeshifter.SwarmTacticalRole;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.warroom.SwarmPartyCommandEvent;
import org.sokybot.swarm.api.warroom.SwarmWarRoomTriggerEvent;
import org.sokybot.warroom.api.IWarRoomSolver;
import org.sokybot.warroom.domain.PartyRosterSolution;
import org.sokybot.warroom.domain.SwarmBotEntity;
import org.sokybot.warroom.domain.WarRoomParty;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Epic #23 Phase 4: maps running machines to solver entities, runs Timefold, publishes party join commands.
 */
@Component(immediate = true)
public final class WarRoomOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(WarRoomOrchestrator.class);

    private static final String REQUESTER = "war-room";

    @Reference
    private ISwarmEventBus swarmEventBus;

    @Reference
    private ISokybotContext sokybotContext;

    @Reference
    private IWarRoomSolver warRoomSolver;

    private volatile Disposable triggerSub;

    @Activate
    void activate() {
        ISwarmEventBus bus = swarmEventBus;
        if (bus == null) {
            log.warn("WarRoomOrchestrator: ISwarmEventBus unavailable");
            return;
        }
        triggerSub = bus.observe(SwarmWarRoomTriggerEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "WarRoomOrchestrator trigger stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::onTrigger);
    }

    @Deactivate
    void deactivate() {
        dispose(triggerSub);
        triggerSub = null;
    }

    private static void dispose(Disposable d) {
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private void onTrigger(SwarmWarRoomTriggerEvent event) {
        if (event == null) {
            return;
        }
        ISokybotContext ctx = sokybotContext;
        IWarRoomSolver solver = warRoomSolver;
        ISwarmEventBus bus = swarmEventBus;
        if (ctx == null || solver == null || bus == null) {
            return;
        }
        String swarmGroupId = event.getSwarmGroupId();
        IGroupContext group = null;
        try {
            for (IGroupContext g : ctx.getGroups()) {
                if (g != null && Objects.equals(g.name().trim(), swarmGroupId)) {
                    group = g;
                    break;
                }
            }
        } catch (Exception ex) {
            log.warn("WarRoomOrchestrator: failed to resolve group {}: {}", swarmGroupId, ex.getMessage());
            return;
        }
        if (group == null) {
            log.debug("WarRoomOrchestrator: no group named {}", swarmGroupId);
            return;
        }

        List<IMachineContext> running = new ArrayList<>();
        try {
            for (IMachineContext m : group.getMachines()) {
                if (m != null && m.isRunning()) {
                    running.add(m);
                }
            }
        } catch (Exception ex) {
            log.warn("WarRoomOrchestrator: failed to list machines: {}", ex.getMessage());
            return;
        }

        List<SwarmBotEntity> botEntities = new ArrayList<>();
        for (IMachineContext machine : running) {
            try {
                SwarmBotEntity entity = mapMachineToEntity(machine);
                if (entity != null) {
                    botEntities.add(entity);
                }
            } catch (Exception ex) {
                log.warn(
                        "WarRoomOrchestrator: skip machine {}: {}",
                        machine != null ? machine.fullName() : "?",
                        ex.getMessage());
            }
        }

        if (botEntities.isEmpty()) {
            log.debug("WarRoomOrchestrator: no bots mapped for {}", swarmGroupId);
            return;
        }

        int numberOfParties = Math.max(1, (int) Math.ceil(botEntities.size() / 8.0));

        solver.calculateOptimalRoster(botEntities, numberOfParties)
                .doOnError(err -> log.warn("WarRoomOrchestrator: solver failed: {}", err.toString()))
                .onErrorResume(err -> Mono.empty())
                .subscribe(
                        solution -> {
                            try {
                                publishPartyCommands(bus, solution);
                            } catch (Exception ex) {
                                log.warn("WarRoomOrchestrator: publish failed: {}", ex.getMessage());
                            }
                        },
                        err -> log.warn("WarRoomOrchestrator: solver subscribe error: {}", err.toString()));
    }

    private static SwarmBotEntity mapMachineToEntity(IMachineContext machine) {
        String machineId = machine.fullName();
        int level = 1;
        try {
            IEngine engine = machine.getEngine();
            if (engine != null) {
                Optional<IWorkflowContext> wctx = engine.optionalWorkflowContext();
                if (wctx.isPresent()) {
                    IGameModel gm = wctx.get().getGameModel();
                    if (gm != null) {
                        ITrainer tr = gm.getTrainer();
                        if (tr != null) {
                            level = Byte.toUnsignedInt(tr.getLevel());
                            if (level <= 0) {
                                level = 1;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            level = 1;
        }

        SwarmTacticalRole role = SwarmTacticalRole.DPS;
        try {
            IEngine engine = machine.getEngine();
            if (engine != null) {
                Optional<IWorkflowContext> wctx = engine.optionalWorkflowContext();
                if (wctx.isPresent()) {
                    IGameModel gm = wctx.get().getGameModel();
                    if (gm != null && gm.getTrainer() != null) {
                        String job = gm.getTrainer().getJobName();
                        if (job != null && job.toUpperCase(Locale.ROOT).contains("CLERIC")) {
                            role = SwarmTacticalRole.HEALER;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            role = SwarmTacticalRole.DPS;
        }

        SwarmBotEntity e = new SwarmBotEntity();
        e.setMachineId(machineId);
        e.setLevel(level);
        e.setRole(role);
        e.setDpsScore(level * 10);
        e.setAssignedParty(null);
        return e;
    }

    private void publishPartyCommands(ISwarmEventBus bus, PartyRosterSolution solution) {
        if (solution == null || bus == null) {
            return;
        }
        List<WarRoomParty> parties = solution.getPartyList();
        List<SwarmBotEntity> bots = solution.getBotList();
        if (parties == null || bots == null) {
            return;
        }

        long ts = System.currentTimeMillis();
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

            for (SwarmBotEntity bot : inParty) {
                if (Objects.equals(bot.getMachineId(), leader.getMachineId())) {
                    continue;
                }
                String reqId = UUID.randomUUID().toString();
                SwarmPartyCommandEvent cmd = new SwarmPartyCommandEvent(
                        REQUESTER,
                        ts,
                        reqId,
                        bot.getMachineId(),
                        leader.getMachineId());
                try {
                    bus.publish(cmd);
                } catch (Exception ex) {
                    log.warn(
                            "WarRoomOrchestrator: publish SwarmPartyCommandEvent failed for {}: {}",
                            bot.getMachineId(),
                            ex.getMessage());
                }
            }
        }
    }
}
