package org.sokybot.behaviors.swarm.warroom;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.sokybot.warroom.api.SwarmBotDto;
import org.sokybot.warroom.api.WarRoomPlan;

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

        List<SwarmBotDto> botDtos = new ArrayList<>();
        for (IMachineContext machine : running) {
            try {
                SwarmBotDto dto = mapMachineToDto(machine);
                if (dto != null) {
                    botDtos.add(dto);
                }
            } catch (Exception ex) {
                log.warn(
                        "WarRoomOrchestrator: skip machine {}: {}",
                        machine != null ? machine.fullName() : "?",
                        ex.getMessage());
            }
        }

        if (botDtos.isEmpty()) {
            log.debug("WarRoomOrchestrator: no bots mapped for {}", swarmGroupId);
            return;
        }

        int numberOfParties = Math.max(1, (int) Math.ceil(botDtos.size() / 8.0));

        solver.calculateOptimalRoster(botDtos, numberOfParties)
                .doOnError(err -> log.warn("WarRoomOrchestrator: solver failed: {}", err.toString()))
                .onErrorResume(err -> Mono.empty())
                .subscribe(
                        plan -> {
                            try {
                                publishPartyCommands(bus, plan);
                            } catch (Exception ex) {
                                log.warn("WarRoomOrchestrator: publish failed: {}", ex.getMessage());
                            }
                        },
                        err -> log.warn("WarRoomOrchestrator: solver subscribe error: {}", err.toString()));
    }

    private static SwarmBotDto mapMachineToDto(IMachineContext machine) {
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

        return new SwarmBotDto(machineId, level, role, level * 10);
    }

    private void publishPartyCommands(ISwarmEventBus bus, WarRoomPlan plan) {
        if (plan == null || bus == null) {
            return;
        }
        Map<String, List<String>> assignments = plan.getPartyAssignments();
        if (assignments == null || assignments.isEmpty()) {
            return;
        }

        long ts = System.currentTimeMillis();
        for (Map.Entry<String, List<String>> entry : assignments.entrySet()) {
            String leaderId = entry.getKey();
            List<String> members = entry.getValue();
            if (leaderId == null || members == null) {
                continue;
            }
            for (String memberId : members) {
                if (memberId == null || memberId.equals(leaderId)) {
                    continue;
                }
                String reqId = UUID.randomUUID().toString();
                SwarmPartyCommandEvent cmd = new SwarmPartyCommandEvent(
                        REQUESTER,
                        ts,
                        reqId,
                        memberId,
                        leaderId);
                try {
                    bus.publish(cmd);
                } catch (Exception ex) {
                    log.warn(
                            "WarRoomOrchestrator: publish SwarmPartyCommandEvent failed for {}: {}",
                            memberId,
                            ex.getMessage());
                }
            }
        }
    }
}
