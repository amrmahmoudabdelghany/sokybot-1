package org.sokybot.behaviors.swarm.symphony;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.behaviors.combat.internal.settings.symphony.ComboDefinition;
import org.sokybot.behaviors.combat.internal.settings.symphony.SymphonyComboSettings;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gameevents.events.buff.BuffAppliedEvent;
import org.sokybot.gameevents.events.skill.SkillCastEvent;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.model.ISpawn;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.symphony.SwarmCombatEffectEvent;
import org.sokybot.swarm.api.symphony.SwarmCombatIntentEvent;

import reactor.core.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Epic #21 Phase 2: publishes symphony swarm events from reactive skill/buff telemetry.
 */
@Component(immediate = true)
public final class SymphonyCombatTelemetryPublisher {

    private static final Logger log = LoggerFactory.getLogger(SymphonyCombatTelemetryPublisher.class);

    @Reference
    private IReactiveEventBus reactiveEventBus;

    @Reference
    private ISwarmEventBus swarmEventBus;

    @Reference
    private ISettingsRegistry settingsRegistry;

    @Reference
    private ISokybotContext sokybotContext;

    private volatile Disposable skillCastSub;
    private volatile Disposable buffAppliedSub;

    @Activate
    void activate() {
        IReactiveEventBus bus = reactiveEventBus;
        if (bus == null) {
            log.warn("SymphonyCombatTelemetryPublisher: IReactiveEventBus unavailable");
            return;
        }
        skillCastSub = bus.on(SkillCastEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "SymphonyCombatTelemetryPublisher SkillCast stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::onSkillCast);
        buffAppliedSub = bus.on(BuffAppliedEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "SymphonyCombatTelemetryPublisher BuffApplied stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::onBuffApplied);
    }

    @Deactivate
    void deactivate() {
        dispose(skillCastSub);
        dispose(buffAppliedSub);
        skillCastSub = null;
        buffAppliedSub = null;
    }

    private static void dispose(Disposable d) {
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private void onSkillCast(SkillCastEvent event) {
        if (event == null || !event.isSuccess()) {
            return;
        }
        Integer skillIdBox = event.getSkillId();
        if (skillIdBox == null) {
            return;
        }
        int skillRefId = skillIdBox.intValue();
        Integer targetBox = event.getTargetId();
        if (targetBox == null) {
            return;
        }
        int targetEntityId = targetBox.intValue();

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
                    maybePublishIntent(event, skillRefId, targetEntityId, registry, swarm, machine);
                } catch (Exception ex) {
                    log.trace(
                            "SymphonyCombatTelemetryPublisher intent skip {}: {}",
                            machine.fullName(),
                            ex.getMessage());
                }
            }
        }
    }

    private static void maybePublishIntent(
            SkillCastEvent event,
            int skillRefId,
            int targetEntityId,
            ISettingsRegistry registry,
            ISwarmEventBus swarm,
            IMachineContext machine) {
        if (!machine.isRunning()) {
            return;
        }
        if (!event.getFullName().equals(machine.fullName())) {
            return;
        }
        SymphonyComboSettings settings = readSymphony(registry, machine);
        if (settings == null || !settings.isSymphonyEnabled()) {
            return;
        }
        List<ComboDefinition> combos = settings.getCombos();
        if (combos == null || combos.isEmpty()) {
            return;
        }
        ComboDefinition match = null;
        for (ComboDefinition c : combos) {
            if (c != null && c.getComboId() != null && !c.getComboId().trim().isEmpty()
                    && c.getTriggerSkillRefId() == skillRefId) {
                match = c;
                break;
            }
        }
        if (match == null) {
            return;
        }

        IGameModel gm = machine.getGameModel();
        if (gm == null) {
            return;
        }
        Optional<ISpawn> spawnOpt = gm.find(targetEntityId);
        if (!spawnOpt.isPresent()) {
            return;
        }
        ISpawn spawn = spawnOpt.get();
        int targetRefId = spawn.getRefId();
        WorldPoint approx = worldPointFromSpawn(spawn);
        long now = System.currentTimeMillis();

        swarm.publish(new SwarmCombatIntentEvent(
                machine.fullName(),
                now,
                UUID.randomUUID().toString(),
                match.getComboId().trim(),
                skillRefId,
                targetRefId,
                approx,
                now));
    }

    private void onBuffApplied(BuffAppliedEvent event) {
        if (event == null) {
            return;
        }
        int buffId = event.getBuffId();
        // Translator (combat.groovy 0x30BD) passes packet target entity id into BuffAppliedEvent.getCasterId().
        int buffTargetEntityId = event.getCasterId();

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
                    maybePublishEffect(event, buffId, buffTargetEntityId, registry, swarm, machine);
                } catch (Exception ex) {
                    log.trace(
                            "SymphonyCombatTelemetryPublisher effect skip {}: {}",
                            machine.fullName(),
                            ex.getMessage());
                }
            }
        }
    }

    private static void maybePublishEffect(
            BuffAppliedEvent event,
            int buffId,
            int buffTargetEntityId,
            ISettingsRegistry registry,
            ISwarmEventBus swarm,
            IMachineContext machine) {
        if (!machine.isRunning()) {
            return;
        }
        if (!event.getFullName().equals(machine.fullName())) {
            return;
        }
        SymphonyComboSettings settings = readSymphony(registry, machine);
        if (settings == null || !settings.isSymphonyEnabled()) {
            return;
        }
        List<ComboDefinition> combos = settings.getCombos();
        if (combos == null || combos.isEmpty()) {
            return;
        }
        ComboDefinition match = null;
        for (ComboDefinition c : combos) {
            if (c != null && c.getComboId() != null && !c.getComboId().trim().isEmpty()
                    && c.getExpectedStatusRefId() == buffId) {
                match = c;
                break;
            }
        }
        if (match == null) {
            return;
        }

        IGameModel gm = machine.getGameModel();
        if (gm == null) {
            return;
        }
        Optional<ISpawn> spawnOpt = gm.find(buffTargetEntityId);
        if (!spawnOpt.isPresent()) {
            return;
        }
        ISpawn spawn = spawnOpt.get();
        int targetRefId = spawn.getRefId();
        WorldPoint approx = worldPointFromSpawn(spawn);
        long now = System.currentTimeMillis();

        swarm.publish(new SwarmCombatEffectEvent(
                machine.fullName(),
                now,
                UUID.randomUUID().toString(),
                match.getComboId().trim(),
                targetRefId,
                buffId,
                approx,
                now));
    }

    private static WorldPoint worldPointFromSpawn(ISpawn spawn) {
        GamePosition p = spawn.getPosition();
        if (p != null) {
            return new WorldPoint(p.getX(), p.getY(), p.getZ());
        }
        return new WorldPoint(spawn.getX(), spawn.getY(), spawn.getZOffset());
    }

    private static SymphonyComboSettings readSymphony(ISettingsRegistry registry, IMachineContext machine) {
        try {
            ISettingsProvider<SymphonyComboSettings> p = registry.getProvider(
                    machine.getGroupName(),
                    machine.getMachineName(),
                    "symphony",
                    SymphonyComboSettings.class);
            return p != null ? p.get() : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
