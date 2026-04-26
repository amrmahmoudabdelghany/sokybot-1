package org.sokybot.behaviors.swarm.radar;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.events.entity.EntitySpawnEvent;
import org.sokybot.gameevents.events.world.GameNotifyEvent;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.persistence.entities.MonsterSpawnPointEntity;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.ISwarmRadarPolicy;
import org.sokybot.swarm.api.SwarmEntityDetectedEvent;

import reactor.core.Disposable;

@Component(immediate = true, service = SwarmRadarComponent.class)
public final class SwarmRadarComponent {

    private static final long DEFAULT_EXPIRES_MS = 120_000L;

    @Reference
    private IReactiveEventBus eventBus;

    @Reference
    private ISwarmEventBus swarmBus;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IGameDataLookup gameData;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISwarmRadarPolicy policyOverride;

    private Disposable notifySub;
    private Disposable spawnSub;

    @Activate
    void activate() {
        notifySub = eventBus.on(GameNotifyEvent.class).subscribe(this::onNotify);
        spawnSub = eventBus.on(EntitySpawnEvent.class).subscribe(this::onSpawn);
    }

    @Deactivate
    void deactivate() {
        dispose(notifySub);
        dispose(spawnSub);
    }

    private static void dispose(Disposable d) {
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private void onNotify(GameNotifyEvent ev) {
        if (ev.getType() != GameNotifyEvent.NotifyType.UNIQUE_SPAWNED) {
            return;
        }
        IGameDataLookup lookup = gameData;
        if (lookup == null || !swarmBus.tryAnnounceRefId(ev.getModelId(), dedupeCooldownMs())) {
            return;
        }
        List<WorldPoint> spawnPoints = lookupCanonicalSpawnPoints(lookup, ev.getModelId());
        if (spawnPoints.isEmpty()) {
            return;
        }
        String displayName = lookup.findNPC(ev.getModelId()).map(NPCEntity::getName).orElse(null);
        WorldPoint primary = spawnPoints.get(0);
        swarmBus.publish(new SwarmEntityDetectedEvent(
                ev.getFullName(),
                ev.getTimestamp(),
                UUID.randomUUID().toString(),
                ev.getModelId(),
                displayName,
                SwarmEntityDetectedEvent.DetectionKind.UNIQUE_GLOBAL_NOTIFY,
                spawnPoints,
                packSector(primary),
                System.currentTimeMillis() + DEFAULT_EXPIRES_MS));
    }

    private void onSpawn(EntitySpawnEvent ev) {
        IGameDataLookup lookup = gameData;
        if (lookup == null || !broadcastVisualConfirms() || !isWatchedOrUnique(lookup, ev.getRefId())) {
            return;
        }
        if (!swarmBus.tryAnnounceRefId(ev.getRefId(), dedupeCooldownMs())) {
            return;
        }
        WorldPoint here = new WorldPoint(
                ev.getPosition().getX(),
                ev.getPosition().getY(),
                ev.getPosition().getZ());
        swarmBus.publish(new SwarmEntityDetectedEvent(
                ev.getFullName(),
                ev.getTimestamp(),
                UUID.randomUUID().toString(),
                ev.getRefId(),
                ev.getEntityName(),
                watchedRefIds().contains(ev.getRefId())
                        ? SwarmEntityDetectedEvent.DetectionKind.WATCHED_REFID
                        : SwarmEntityDetectedEvent.DetectionKind.UNIQUE_VISUAL_CONFIRM,
                Collections.singletonList(here),
                packSector(here),
                System.currentTimeMillis() + DEFAULT_EXPIRES_MS));
    }

    private List<WorldPoint> lookupCanonicalSpawnPoints(IGameDataLookup lookup, int refId) {
        List<MonsterSpawnPointEntity> points = lookup.findMonsterSpawnPoints(refId);
        return points.stream()
                .map(p -> new WorldPoint(p.getX(), p.getY(), p.getZ()))
                .collect(Collectors.toList());
    }

    private boolean isWatchedOrUnique(IGameDataLookup lookup, int refId) {
        if (watchedRefIds().contains(refId)) {
            return true;
        }
        return lookup.findNPC(refId).map(NPCEntity::isMonster).orElse(false);
    }

    private long dedupeCooldownMs() {
        ISwarmRadarPolicy p = policyOverride;
        return p != null ? p.dedupeCooldownMs() : 30_000L;
    }

    private boolean broadcastVisualConfirms() {
        ISwarmRadarPolicy p = policyOverride;
        return p == null || p.broadcastVisualConfirms();
    }

    private java.util.Set<Integer> watchedRefIds() {
        ISwarmRadarPolicy p = policyOverride;
        return p == null ? Collections.emptySet() : p.watchedRefIds();
    }

    private static int packSector(WorldPoint point) {
        int sectorX = Math.round(point.getX() / 192.0f);
        int sectorY = Math.round(point.getY() / 192.0f);
        return (sectorX << 16) | (sectorY & 0xFFFF);
    }
}
