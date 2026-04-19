package org.sokybot.combat.projections.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.combat.api.IAmmoMonitor;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.events.inventory.AmmoUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;

/**
 * Subscribes to {@link AmmoUpdateEvent} and keeps the latest ammo count per machine.
 */
@Component(service = IAmmoMonitor.class, immediate = true)
public final class AmmoMonitorImpl implements IAmmoMonitor {

    private static final Logger log = LoggerFactory.getLogger(AmmoMonitorImpl.class);

    private static final int AMMO_UNKNOWN = -1;

    private final ConcurrentHashMap<String, Integer> latestCountByMachine = new ConcurrentHashMap<>();
    private final List<Disposable> subscriptions = new ArrayList<>();

    @Reference
    private IReactiveEventBus reactiveEventBus;

    @Activate
    void activate() {
        subscriptions.add(reactiveEventBus.on(AmmoUpdateEvent.class).subscribe(this::onAmmo));
        log.debug("IAmmoMonitor active");
    }

    @Deactivate
    void deactivate() {
        for (Disposable d : subscriptions) {
            if (d != null && !d.isDisposed()) {
                d.dispose();
            }
        }
        subscriptions.clear();
        latestCountByMachine.clear();
    }

    private void onAmmo(AmmoUpdateEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        latestCountByMachine.put(key, Integer.valueOf(e.getAmmoCount()));
    }

    private static String normalize(String machineFullName) {
        if (machineFullName == null) {
            return null;
        }
        String t = machineFullName.trim();
        return t.isEmpty() ? null : t;
    }

    @Override
    public int getAmmoCount(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return AMMO_UNKNOWN;
        }
        Integer v = latestCountByMachine.get(key);
        return v != null ? v.intValue() : AMMO_UNKNOWN;
    }

    @Override
    public boolean hasAmmo(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return true;
        }
        Integer v = latestCountByMachine.get(key);
        if (v == null) {
            return true;
        }
        return v.intValue() > 0;
    }
}
