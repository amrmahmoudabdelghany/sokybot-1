package org.sokybot.combat.projections.internal;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.sokybot.combat.api.ILeashAnchorStore;

/**
 * In-memory leash anchor per machine.
 */
@Component(service = ILeashAnchorStore.class, immediate = true)
public final class LeashAnchorStore implements ILeashAnchorStore {

    private final ConcurrentHashMap<String, float[]> anchorByMachine = new ConcurrentHashMap<>();

    private static String normalize(String machineFullName) {
        if (machineFullName == null) {
            return null;
        }
        String t = machineFullName.trim();
        return t.isEmpty() ? null : t;
    }

    @Override
    public void setAnchor(String machineFullName, float x, float y, float z) {
        String key = normalize(machineFullName);
        if (key == null) {
            return;
        }
        anchorByMachine.put(key, new float[] { x, y, z });
    }

    @Override
    public void clearAnchor(String machineFullName) {
        String key = normalize(machineFullName);
        if (key != null) {
            anchorByMachine.remove(key);
        }
    }

    @Override
    public Optional<float[]> getAnchor(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return Optional.empty();
        }
        float[] a = anchorByMachine.get(key);
        return a == null ? Optional.empty() : Optional.of(new float[] { a[0], a[1], a[2] });
    }
}
