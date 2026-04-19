package org.sokybot.party.projections.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.commons.SilkroadUtils;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gameevents.events.character.CharacterLoadedEvent;
import org.sokybot.gameevents.events.entity.EntityMovementEvent;
import org.sokybot.party.api.IPartyMachineDirectory;
import org.sokybot.party.api.PartyMachineEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;

/**
 * Tracks each machine's own character identity and coarse world position from character load + movement events.
 */
@Component(service = IPartyMachineDirectory.class, immediate = true)
public final class PartyDirectoryImpl implements IPartyMachineDirectory {

    private static final Logger log = LoggerFactory.getLogger(PartyDirectoryImpl.class);

    private final ConcurrentMap<String, DirEntry> byMachine = new ConcurrentHashMap<>();
    private final List<Disposable> subscriptions = new ArrayList<>();

    @Reference
    private IReactiveEventBus reactiveEventBus;

    @Activate
    void activate() {
        subscriptions.add(reactiveEventBus.on(CharacterLoadedEvent.class).subscribe(this::onCharacterLoaded));
        subscriptions.add(reactiveEventBus.on(EntityMovementEvent.class).subscribe(this::onEntityMovement));
        log.debug("Party directory active");
    }

    @Deactivate
    void deactivate() {
        for (Disposable d : subscriptions) {
            if (d != null && !d.isDisposed()) {
                d.dispose();
            }
        }
        subscriptions.clear();
        byMachine.clear();
    }

    @Override
    public Optional<PartyMachineEntry> lookup(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return Optional.empty();
        }
        DirEntry e = byMachine.get(key);
        if (e == null || e.entityId <= 0) {
            return Optional.empty();
        }
        return Optional.of(new PartyMachineEntry(e.entityId, e.charName, e.x, e.y, e.z));
    }

    private static String normalize(String machineFullName) {
        if (machineFullName == null) {
            return null;
        }
        String t = machineFullName.trim();
        return t.isEmpty() ? null : t;
    }

    private void onCharacterLoaded(CharacterLoadedEvent ev) {
        String key = normalize(ev.getFullName());
        if (key == null) {
            return;
        }
        DirEntry e = byMachine.computeIfAbsent(key, k -> new DirEntry());
        e.entityId = ev.getUniqueId();
        e.charName = ev.getCharacterName() != null ? ev.getCharacterName() : "";
        float x = SilkroadUtils.getXCoord(ev.getXOffset(), (short) ev.getXSector());
        float y = SilkroadUtils.getYCoord(ev.getYOffset(), (short) ev.getYSector());
        float z = ev.getZOffset();
        e.x = x;
        e.y = y;
        e.z = z;
    }

    private void onEntityMovement(EntityMovementEvent ev) {
        String key = normalize(ev.getFullName());
        if (key == null) {
            return;
        }
        DirEntry e = byMachine.get(key);
        if (e == null || e.entityId != ev.getEntityId()) {
            return;
        }
        GamePosition cp = ev.getCurrentPosition();
        if (cp != null) {
            applyGamePosition(e, cp);
            return;
        }
        GamePosition dest = ev.getDestination();
        if (dest != null && ev.hasDestination()) {
            applyGamePosition(e, dest);
        }
    }

    private static void applyGamePosition(DirEntry e, GamePosition p) {
        e.x = p.getX();
        e.y = p.getY();
        e.z = p.getZ();
    }

    private static final class DirEntry {
        volatile int entityId;
        volatile String charName = "";
        volatile float x;
        volatile float y;
        volatile float z;
    }
}
