package org.sokybot.behaviors.swarm.sentinel;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.behaviors.combat.internal.settings.SentinelSettings;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.events.entity.EntityHPMPUpdateEvent;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.model.IPlayer;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmHostilePlayerEvent;

import reactor.core.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Epic #20 Phase 2 (Watchman): detects trainer HP loss and publishes {@link SwarmHostilePlayerEvent}
 * for probable hostile players (targeting heuristic, then closest-player fallback).
 */
@Component(immediate = true, name = SentinelDamagePublisher.COMPONENT_NAME)
public final class SentinelDamagePublisher {

    static final String COMPONENT_NAME = "org.sokybot.behaviors.swarm.sentinel.SentinelDamagePublisher";

    private static final Logger log = LoggerFactory.getLogger(SentinelDamagePublisher.class);

    private static final float CLOSEST_HOSTILE_RADIUS = 50.0f;

    private final Map<String, Integer> lastHpCache = new ConcurrentHashMap<>();

    @Reference
    private IReactiveEventBus reactiveEventBus;

    @Reference
    private ISwarmEventBus swarmEventBus;

    @Reference
    private ISokybotContext sokybotContext;

    @Reference
    private ISettingsRegistry settingsRegistry;

    private volatile Disposable hpSubscription;

    @Activate
    void activate() {
        IReactiveEventBus bus = reactiveEventBus;
        if (bus == null) {
            log.warn("SentinelDamagePublisher: IReactiveEventBus unavailable");
            return;
        }
        hpSubscription = bus.on(EntityHPMPUpdateEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "SentinelDamagePublisher HP stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::onHpMp);
    }

    @Deactivate
    void deactivate() {
        Disposable d = hpSubscription;
        hpSubscription = null;
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private void onHpMp(EntityHPMPUpdateEvent event) {
        if (event == null) {
            return;
        }
        Integer newHpBox = event.getNewHP();
        if (newHpBox == null) {
            return;
        }
        int currentHp = newHpBox.intValue();

        ISokybotContext ctx = sokybotContext;
        ISettingsRegistry registry = settingsRegistry;
        ISwarmEventBus swarm = swarmEventBus;
        if (ctx == null || registry == null || swarm == null) {
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
                    evaluateMachine(event, currentHp, registry, swarm, machine);
                } catch (Exception ex) {
                    log.trace(
                            "SentinelDamagePublisher skip machine {}: {}",
                            machine.fullName(),
                            ex.getMessage());
                }
            }
        }
    }

    private void evaluateMachine(
            EntityHPMPUpdateEvent event,
            int currentHp,
            ISettingsRegistry registry,
            ISwarmEventBus swarm,
            IMachineContext machine) {
        if (!machine.isRunning()) {
            return;
        }
        if (!event.getFullName().equals(machine.fullName())) {
            return;
        }

        IGameModel gameModel = machine.getGameModel();
        if (gameModel == null) {
            return;
        }
        ITrainer trainer = gameModel.getTrainer();
        if (trainer == null) {
            return;
        }
        int selfEntityId = trainer.getUniqueId();
        if (event.getEntityId() != selfEntityId) {
            return;
        }

        SentinelSettings settings = readSentinel(registry, machine);
        if (settings == null || !settings.isSentinelEnabled()) {
            lastHpCache.put(machine.fullName(), Integer.valueOf(currentHp));
            return;
        }

        int previousHp = lastHpCache.getOrDefault(machine.fullName(), Integer.valueOf(currentHp)).intValue();

        if (currentHp < previousHp) {
            IPlayer culprit = findHostilePlayer(gameModel, selfEntityId, trainer);
            if (culprit != null) {
                String name = culprit.getName();
                if (name != null && !name.trim().isEmpty()) {
                    long now = System.currentTimeMillis();
                    swarm.publish(new SwarmHostilePlayerEvent(
                            machine.fullName(),
                            now,
                            UUID.randomUUID().toString(),
                            name,
                            now));
                }
            }
        }

        lastHpCache.put(machine.fullName(), Integer.valueOf(currentHp));
    }

    /**
     * Prefer a player whose combat target is the victim; otherwise the closest other {@link IPlayer}
     * within {@link #CLOSEST_HOSTILE_RADIUS}.
     */
    private static IPlayer findHostilePlayer(IGameModel gameModel, int selfEntityId, ITrainer trainer) {
        List<IPlayer> players = gameModel.snapshotAll(IPlayer.class);
        if (players == null || players.isEmpty()) {
            return null;
        }

        for (IPlayer player : players) {
            if (player == null || player.getUniqueId() == selfEntityId) {
                continue;
            }
            if (!player.isAlive()) {
                continue;
            }
            if (player.getTargetId() == selfEntityId) {
                return player;
            }
        }

        IPlayer closest = null;
        double bestDist = Double.MAX_VALUE;
        for (IPlayer player : players) {
            if (player == null || player.getUniqueId() == selfEntityId) {
                continue;
            }
            if (!player.isAlive()) {
                continue;
            }
            double d = trainer.distance(player.getX(), player.getY());
            if (d <= CLOSEST_HOSTILE_RADIUS && d < bestDist) {
                bestDist = d;
                closest = player;
            }
        }
        return closest;
    }

    private static SentinelSettings readSentinel(ISettingsRegistry registry, IMachineContext machine) {
        try {
            ISettingsProvider<SentinelSettings> p = registry.getProvider(
                    machine.getGroupName(),
                    machine.getMachineName(),
                    "sentinel",
                    SentinelSettings.class);
            return p != null ? p.get() : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
