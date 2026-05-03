package org.sokybot.behaviors.swarm.shapeshifter;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.behaviors.party.internal.settings.shapeshifter.FallbackRule;
import org.sokybot.behaviors.party.internal.settings.shapeshifter.ShapeshifterSettings;
import org.sokybot.party.api.IPartyModel;
import org.sokybot.party.api.IPartySnapshot;
import org.sokybot.party.api.PartyClass;
import org.sokybot.party.api.PartyMember;
import org.sokybot.party.api.shapeshifter.SwarmTacticalRole;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.shapeshifter.SwarmRoleDeficitEvent;
import org.sokybot.swarm.api.shapeshifter.SwarmRoleRestoredEvent;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Epic #22 Phase 2: polls party snapshots per group and publishes deficit/restored swarm events.
 */
@Component(immediate = true)
public final class ShapeshifterRoleWatchCoordinator {

    private static final Logger log = LoggerFactory.getLogger(ShapeshifterRoleWatchCoordinator.class);

    private final ConcurrentHashMap<String, Long> roleLostTimestamps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> currentlyDeficient = new ConcurrentHashMap<>();

    @Reference
    private ISwarmEventBus swarmEventBus;

    @Reference
    private ISokybotContext sokybotContext;

    @Reference
    private ISettingsRegistry settingsRegistry;

    @Reference
    private IPartyModel partyModel;

    private volatile Disposable pollDisposable;

    @Activate
    void activate() {
        pollDisposable = Flux.interval(Duration.ofSeconds(1L))
                .publishOn(Schedulers.boundedElastic())
                .subscribe(
                        tick -> {
                            try {
                                watchGroups();
                            } catch (Exception ex) {
                                log.trace("ShapeshifterRoleWatchCoordinator tick: {}", ex.getMessage());
                            }
                        },
                        err -> log.warn("ShapeshifterRoleWatchCoordinator flux error: {}", err.toString()));
    }

    @Deactivate
    void deactivate() {
        Disposable d = pollDisposable;
        pollDisposable = null;
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
        roleLostTimestamps.clear();
        currentlyDeficient.clear();
    }

    void watchGroups() {
        ISokybotContext ctx = sokybotContext;
        if (ctx == null) {
            return;
        }
        for (IGroupContext group : ctx.getGroups()) {
            if (group == null) {
                continue;
            }
            try {
                watchGroup(group);
            } catch (Exception ex) {
                log.trace("ShapeshifterRoleWatchCoordinator group {}: {}", group.name(), ex.getMessage());
            }
        }
    }

    private void watchGroup(IGroupContext group) {
        IMachineContext machine = Arrays.stream(group.getMachines())
                .filter(Objects::nonNull)
                .filter(IMachineContext::isRunning)
                .findFirst()
                .orElse(null);
        if (machine == null) {
            return;
        }

        ISettingsRegistry registry = settingsRegistry;
        ISwarmEventBus bus = swarmEventBus;
        IPartyModel parties = partyModel;
        if (registry == null || bus == null || parties == null) {
            return;
        }

        ShapeshifterSettings settings = readShapeshifter(registry, machine);
        if (settings == null || !settings.isShapeshifterEnabled()) {
            return;
        }

        Optional<IPartySnapshot> snapOpt = parties.snapshot(machine.fullName());
        if (!snapOpt.isPresent()) {
            return;
        }
        IPartySnapshot snapshot = snapOpt.get();

        Set<SwarmTacticalRole> coveredRoles = buildCoveredRoles(snapshot);

        List<FallbackRule> rules = settings.getRules();
        if (rules == null || rules.isEmpty()) {
            return;
        }

        String swarmGroupId = group.name().trim();
        long now = System.currentTimeMillis();
        long settleMs = settings.getSettleDelayMs();

        for (FallbackRule rule : rules) {
            if (rule == null) {
                continue;
            }
            SwarmTacticalRole requiredRole = rule.getDeficitRole();
            if (requiredRole == null || requiredRole == SwarmTacticalRole.UNKNOWN) {
                continue;
            }

            String cacheKey = swarmGroupId + "_" + requiredRole.name();
            boolean covered = coveredRoles.contains(requiredRole);

            if (!covered) {
                roleLostTimestamps.putIfAbsent(cacheKey, Long.valueOf(now));
                Long lostAt = roleLostTimestamps.get(cacheKey);
                boolean alreadyBroadcast = Boolean.TRUE.equals(currentlyDeficient.get(cacheKey));
                if (lostAt != null && now - lostAt.longValue() >= settleMs && !alreadyBroadcast) {
                    currentlyDeficient.put(cacheKey, Boolean.TRUE);
                    bus.publish(new SwarmRoleDeficitEvent(
                            machine.fullName(),
                            now,
                            UUID.randomUUID().toString(),
                            requiredRole,
                            swarmGroupId,
                            ""));
                }
            } else {
                roleLostTimestamps.remove(cacheKey);
                Boolean prev = currentlyDeficient.put(cacheKey, Boolean.FALSE);
                if (Boolean.TRUE.equals(prev)) {
                    bus.publish(new SwarmRoleRestoredEvent(
                            machine.fullName(),
                            now,
                            UUID.randomUUID().toString(),
                            requiredRole,
                            swarmGroupId,
                            ""));
                }
            }
        }
    }

    /**
     * Builds the set of tactical roles currently represented by alive (or presence-only) party members.
     * <p>
     * When {@link PartyMember#getHpPercent()} is negative (unknown per API), we treat the member as alive if they
     * appear in the roster — otherwise we could never detect restoration from uncertain HP telemetry.
     * </p>
     */
    private static Set<SwarmTacticalRole> buildCoveredRoles(IPartySnapshot snapshot) {
        Set<SwarmTacticalRole> covered = new HashSet<>();
        List<PartyMember> members = snapshot.getMembers();
        if (members == null) {
            return covered;
        }
        for (PartyMember m : members) {
            if (m == null) {
                continue;
            }
            if (!memberAppearsAlive(m)) {
                continue;
            }
            SwarmTacticalRole role = mapPartyClass(m.getPartyClass());
            if (role != SwarmTacticalRole.UNKNOWN) {
                covered.add(role);
            }
        }
        return covered;
    }

    /**
     * Uses {@link PartyMember#getHpPercent()} when non-negative; otherwise assumes healthy if listed as a member
     * (HP telemetry gap — see {@link PartyMember} javadoc for unknown hp).
     */
    private static boolean memberAppearsAlive(PartyMember m) {
        int hp = m.getHpPercent();
        if (hp < 0) {
            return true;
        }
        return hp > 0;
    }

    private static SwarmTacticalRole mapPartyClass(PartyClass partyClass) {
        if (partyClass == null || partyClass == PartyClass.UNKNOWN) {
            return SwarmTacticalRole.DPS;
        }
        switch (partyClass) {
            case CLERIC:
                return SwarmTacticalRole.HEALER;
            case BARD:
                return SwarmTacticalRole.BUFFER;
            case WARRIOR:
                return SwarmTacticalRole.TANK;
            case WIZARD:
            case WARLOCK:
            case ROGUE:
            case MIXED:
            default:
                return SwarmTacticalRole.DPS;
        }
    }

    private static ShapeshifterSettings readShapeshifter(ISettingsRegistry registry, IMachineContext machine) {
        try {
            ISettingsProvider<ShapeshifterSettings> p = registry.getProvider(
                    machine.getGroupName(),
                    machine.getMachineName(),
                    "shapeshifter",
                    ShapeshifterSettings.class);
            return p != null ? p.get() : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
