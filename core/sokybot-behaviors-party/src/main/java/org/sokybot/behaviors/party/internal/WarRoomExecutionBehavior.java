package org.sokybot.behaviors.party.internal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.behaviors.party.internal.settings.PartySettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.party.api.IPartyModel;
import org.sokybot.party.api.IPartySnapshot;
import org.sokybot.party.api.PartyCycleKeys;
import org.sokybot.party.api.PartyMember;
import org.sokybot.party.coordination.api.IPartyCoordinator;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.warroom.SwarmPartyCommandEvent;

import reactor.core.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Epic #23 Phase 4: executes War Room party commands (leave wrong party, invite/join target leader).
 */
@Component(service = IBehavior.class, immediate = true, property = "order=2")
public final class WarRoomExecutionBehavior implements IBehavior<PartySettings> {

    private static final Logger log = LoggerFactory.getLogger(WarRoomExecutionBehavior.class);

    private static final String BEHAVIOR_ID = "party.warRoomExecution";

    private final Map<String, String> pendingCommands = new ConcurrentHashMap<>();

    @Reference
    private ISwarmEventBus swarmEventBus;

    @Reference
    private ISokybotContext sokybotContext;

    @Reference
    private IPartyModel partyModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IPartyCoordinator partyCoordinator;

    private volatile Disposable partyCmdSub;

    @Activate
    void activate() {
        ISwarmEventBus bus = swarmEventBus;
        if (bus == null) {
            log.warn("WarRoomExecutionBehavior: ISwarmEventBus unavailable");
            return;
        }
        partyCmdSub = bus.observe(SwarmPartyCommandEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "WarRoomExecutionBehavior party-command stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::onPartyCommand);
    }

    @Deactivate
    void deactivate() {
        if (partyCmdSub != null && !partyCmdSub.isDisposed()) {
            partyCmdSub.dispose();
        }
        partyCmdSub = null;
    }

    private void onPartyCommand(SwarmPartyCommandEvent event) {
        if (event == null) {
            return;
        }
        String target = event.getTargetMachineId();
        String leader = event.getTargetLeaderMachineId();
        ISokybotContext ctx = sokybotContext;
        if (ctx == null || target == null || leader == null) {
            return;
        }
        try {
            for (IGroupContext g : ctx.getGroups()) {
                if (g == null) {
                    continue;
                }
                for (IMachineContext m : g.getMachines()) {
                    if (m != null && m.isRunning() && target.equals(m.fullName())) {
                        pendingCommands.put(target, leader);
                        return;
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("WarRoomExecutionBehavior: failed to index party command: {}", ex.getMessage());
        }
    }

    @Override
    public String id() {
        return BEHAVIOR_ID;
    }

    @Override
    public int order() {
        return 2;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return PartyCycleKeys.CYCLE_NAME.equals(cycleId);
    }

    @Override
    public Class<PartySettings> settingsType() {
        return PartySettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, PartySettings settings) {
        return pendingCommands.containsKey(context.getMachineId());
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, PartySettings settings) {
        String machineFullName = context.getMachineId();
        String targetLeaderId = pendingCommands.get(machineFullName);
        if (targetLeaderId == null) {
            return BehaviorStatus.SKIPPED;
        }

        Optional<IPartySnapshot> snapOpt = partyModel.snapshot(machineFullName);
        if (!snapOpt.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        IPartySnapshot snap = snapOpt.get();

        Optional<String> leaderNameOpt = resolveLeaderCharacterName(targetLeaderId);
        String leaderCharName = leaderNameOpt.orElse(null);

        boolean inParty = snap.getPartyId() > 0 && !snap.getMembers().isEmpty();

        if (inParty && leaderCharName != null && rosterContainsCharacter(snap, leaderCharName)) {
            pendingCommands.remove(machineFullName);
            return BehaviorStatus.SKIPPED;
        }

        if (inParty && leaderCharName != null && !rosterContainsCharacter(snap, leaderCharName)) {
            PartyPackets.sendPartyLeave(context);
            return BehaviorStatus.EXECUTED;
        }

        if (!inParty) {
            if (leaderCharName == null || leaderCharName.trim().isEmpty()) {
                return BehaviorStatus.SKIPPED;
            }
            String trimmed = leaderCharName.trim();
            IPartyCoordinator coord = partyCoordinator;
            if (coord != null) {
                try {
                    coord.requestInvite(machineFullName, targetLeaderId);
                } catch (Exception ex) {
                    log.debug("WarRoomExecutionBehavior: coordinator requestInvite failed: {}", ex.getMessage());
                }
            }
            PartyPackets.sendInvite(context, trimmed);
            return BehaviorStatus.EXECUTED;
        }

        return BehaviorStatus.SKIPPED;
    }

    private static boolean rosterContainsCharacter(IPartySnapshot snap, String characterName) {
        if (snap == null || characterName == null) {
            return false;
        }
        String norm = characterName.trim();
        if (norm.isEmpty()) {
            return false;
        }
        for (PartyMember m : snap.getMembers()) {
            if (m != null && norm.equalsIgnoreCase(m.getCharName())) {
                return true;
            }
        }
        return false;
    }

    private Optional<String> resolveLeaderCharacterName(String leaderMachineFullName) {
        if (leaderMachineFullName == null || sokybotContext == null) {
            return Optional.empty();
        }
        try {
            Optional<IMachineContext> machine = findMachineByFullName(leaderMachineFullName);
            if (!machine.isPresent()) {
                return Optional.empty();
            }
            org.sokybot.engine.IEngine engine = machine.get().getEngine();
            if (engine == null) {
                return Optional.empty();
            }
            Optional<IWorkflowContext> wctx = engine.optionalWorkflowContext();
            if (!wctx.isPresent()) {
                return Optional.empty();
            }
            IGameModel gm = wctx.get().getGameModel();
            if (gm == null) {
                return Optional.empty();
            }
            ITrainer tr = gm.getTrainer();
            if (tr == null) {
                return Optional.empty();
            }
            String name = tr.getName();
            if (name == null || name.trim().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(name.trim());
        } catch (Exception ex) {
            log.debug("WarRoomExecutionBehavior: resolve leader name failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<IMachineContext> findMachineByFullName(String fullName) {
        if (fullName == null || sokybotContext == null) {
            return Optional.empty();
        }
        try {
            for (IGroupContext g : sokybotContext.getGroups()) {
                if (g == null) {
                    continue;
                }
                for (IMachineContext m : g.getMachines()) {
                    if (m != null && fullName.equals(m.fullName())) {
                        return Optional.of(m);
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("WarRoomExecutionBehavior: findMachine failed: {}", ex.getMessage());
        }
        return Optional.empty();
    }
}
