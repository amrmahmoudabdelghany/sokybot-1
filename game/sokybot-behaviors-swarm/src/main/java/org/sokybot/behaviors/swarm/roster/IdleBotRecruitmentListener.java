package org.sokybot.behaviors.swarm.roster;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.party.api.IPartyModel;
import org.sokybot.party.api.IPartySnapshot;
import org.sokybot.party.api.PartyMember;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.settings.fleet.FleetMachineSettings;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmRecruitmentBidEvent;
import org.sokybot.swarm.api.SwarmRecruitmentEvent;

import reactor.core.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Idle/solo bots matching a recruitment profile post {@link SwarmRecruitmentBidEvent}s (Epic #17 Phase 3).
 */
@Component(service = IdleBotRecruitmentListener.class, immediate = true)
public final class IdleBotRecruitmentListener {

    private static final Logger log = LoggerFactory.getLogger(IdleBotRecruitmentListener.class);

    @Reference
    private ISwarmEventBus swarmBus;

    @Reference
    private ISokybotContext sokybotContext;

    @Reference
    private ISettingsRegistry settingsRegistry;

    @Reference
    private IPartyModel partyModel;

    private volatile Disposable recruitmentSub;

    @Activate
    void activate() {
        ISwarmEventBus bus = swarmBus;
        if (bus == null) {
            log.warn("IdleBotRecruitmentListener: ISwarmEventBus unavailable");
            return;
        }
        recruitmentSub = bus.observe(SwarmRecruitmentEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "IdleBotRecruitmentListener recruitment stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::onRecruitment);
    }

    @Deactivate
    void deactivate() {
        Disposable d = recruitmentSub;
        recruitmentSub = null;
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private void onRecruitment(SwarmRecruitmentEvent event) {
        if (event == null) {
            return;
        }
        ISokybotContext ctx = sokybotContext;
        ISwarmEventBus bus = swarmBus;
        if (ctx == null || bus == null) {
            return;
        }
        String missing = event.getMissingProfileId();
        if (missing == null || missing.isEmpty()) {
            return;
        }
        for (IGroupContext group : ctx.getGroups()) {
            if (group == null) {
                continue;
            }
            for (IMachineContext machine : group.getMachines()) {
                if (machine == null) {
                    continue;
                }
                try {
                    maybeBid(bus, event, machine, missing);
                } catch (Exception ex) {
                    log.trace(
                            "IdleBotRecruitmentListener skip machine {}: {}",
                            machine.fullName(),
                            ex.getMessage());
                }
            }
        }
    }

    private void maybeBid(ISwarmEventBus bus, SwarmRecruitmentEvent event, IMachineContext machine, String missingProfileId) {
        if (!machine.isRunning()) {
            return;
        }
        String machineFullName = machine.fullName();
        if (machineFullName == null || machineFullName.trim().isEmpty()) {
            return;
        }
        if (Objects.equals(machineFullName, event.getLeaderMachineId())) {
            return;
        }

        FleetMachineSettings fleet = readFleet(machine);
        if (fleet == null) {
            return;
        }
        String profileId = fleet.getProfileId();
        if (profileId == null || profileId.trim().isEmpty() || !missingProfileId.equals(profileId.trim())) {
            return;
        }

        IPartyModel parties = partyModel;
        if (parties == null) {
            return;
        }
        IPartySnapshot snap = parties.snapshot(machineFullName).orElse(null);
        if (snap == null) {
            return;
        }

        String charName = resolveSoloCharName(snap);
        if (charName == null || charName.isEmpty()) {
            return;
        }

        long ts = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        bus.publish(new SwarmRecruitmentBidEvent(
                machineFullName,
                ts,
                requestId,
                event.getRecruitmentId(),
                profileId.trim(),
                charName));
        log.debug(
                "IdleBotRecruitmentListener bid recruitmentId={} bidder={} profile={}",
                event.getRecruitmentId(),
                machineFullName,
                profileId.trim());
    }

    /**
     * Solo / not-in-active-party: at most one roster row and it must match the party leader (or unknown leader).
     */
    private static String resolveSoloCharName(IPartySnapshot snap) {
        List<PartyMember> members = snap.getMembers();
        if (members == null || members.isEmpty()) {
            return null;
        }
        if (members.size() > 1) {
            return null;
        }
        PartyMember only = members.get(0);
        if (only == null) {
            return null;
        }
        int leaderId = snap.getLeaderEntityId();
        if (leaderId > 0 && only.getEntityId() != leaderId) {
            return null;
        }
        String name = only.getCharName();
        return name != null ? name.trim() : null;
    }

    private FleetMachineSettings readFleet(IMachineContext machine) {
        try {
            ISettingsProvider<FleetMachineSettings> provider = settingsRegistry.getProvider(
                    machine.getGroupName(),
                    machine.getMachineName(),
                    FleetMachineSettings.SCOPE_NAME,
                    FleetMachineSettings.class);
            return provider != null ? provider.get() : null;
        } catch (Exception e) {
            log.trace("IdleBotRecruitmentListener fleet read {}: {}", machine.fullName(), e.getMessage());
            return null;
        }
    }
}
