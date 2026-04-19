package org.sokybot.combat.projections.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gameevents.events.character.CharacterLoadedEvent;
import org.sokybot.gameevents.events.entity.EntityMovementEvent;
import org.sokybot.navigation.api.IStuckDetector;
import org.sokybot.navigation.api.MovementSample;
import org.sokybot.navigation.api.WorldPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;

/**
 * Sliding-window path-length stuck detector driven by {@link EntityMovementEvent} for the local character.
 */
@Component(service = IStuckDetector.class, immediate = true)
public final class StuckDetectorImpl implements IStuckDetector {

    private static final Logger log = LoggerFactory.getLogger(StuckDetectorImpl.class);

    private static final long SAMPLE_WINDOW_MS = 4000L;
    private static final int MIN_SAMPLES_FOR_DECISION = 5;
    private static final float STUCK_PATH_LENGTH_MAX = 3.0f;
    private static final int MAX_DEQUE_SIZE = 96;

    private final ConcurrentHashMap<String, Integer> selfEntityByMachine = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ArrayDeque<MovementSample>> samplesByMachine = new ConcurrentHashMap<>();

    private final List<Disposable> subscriptions = new ArrayList<>();

    @Reference
    private IReactiveEventBus reactiveEventBus;

    @Activate
    void activate() {
        subscriptions.add(reactiveEventBus.on(CharacterLoadedEvent.class).subscribe(this::onCharacterLoaded));
        subscriptions.add(reactiveEventBus.on(EntityMovementEvent.class).subscribe(this::onEntityMovement));
        log.debug("IStuckDetector projection active");
    }

    @Deactivate
    void deactivate() {
        for (Disposable d : subscriptions) {
            if (d != null && !d.isDisposed()) {
                d.dispose();
            }
        }
        subscriptions.clear();
        selfEntityByMachine.clear();
        samplesByMachine.clear();
    }

    private static String normalize(String machineFullName) {
        if (machineFullName == null) {
            return null;
        }
        String t = machineFullName.trim();
        return t.isEmpty() ? null : t;
    }

    private void onCharacterLoaded(CharacterLoadedEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        selfEntityByMachine.put(key, Integer.valueOf(e.getUniqueId()));
    }

    private void onEntityMovement(EntityMovementEvent e) {
        String key = normalize(e.getFullName());
        if (key == null) {
            return;
        }
        Integer selfId = selfEntityByMachine.get(key);
        if (selfId == null || e.getEntityId() != selfId.intValue()) {
            return;
        }
        GamePosition cp = e.getCurrentPosition();
        if (cp == null && e.hasDestination()) {
            cp = e.getDestination();
        }
        if (cp == null) {
            return;
        }
        recordPosition(key, cp.getX(), cp.getY(), cp.getZ(), e.getTimestamp());
    }

    @Override
    public void recordPosition(String machineFullName, float x, float y, float z, long epochMillis) {
        String key = normalize(machineFullName);
        if (key == null) {
            return;
        }
        MovementSample sample = new MovementSample(epochMillis, new WorldPoint(x, y, z));
        ArrayDeque<MovementSample> dq = samplesByMachine.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (dq) {
            dq.addLast(sample);
            pruneDeque(dq, epochMillis);
            while (dq.size() > MAX_DEQUE_SIZE) {
                dq.removeFirst();
            }
        }
    }

    private static void pruneDeque(ArrayDeque<MovementSample> dq, long nowMillis) {
        while (!dq.isEmpty() && nowMillis - dq.peekFirst().getEpochMillis() > SAMPLE_WINDOW_MS) {
            dq.removeFirst();
        }
    }

    @Override
    public boolean isStuck(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return false;
        }
        ArrayDeque<MovementSample> dq = samplesByMachine.get(key);
        if (dq == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        synchronized (dq) {
            pruneDeque(dq, now);
            if (dq.size() < MIN_SAMPLES_FOR_DECISION) {
                return false;
            }
            float pathLength = 0f;
            MovementSample prev = null;
            for (MovementSample s : dq) {
                if (prev != null) {
                    pathLength += prev.getPosition().distanceTo(s.getPosition());
                }
                prev = s;
            }
            return pathLength < STUCK_PATH_LENGTH_MAX;
        }
    }

    @Override
    public void reset(String machineFullName) {
        String key = normalize(machineFullName);
        if (key == null) {
            return;
        }
        samplesByMachine.remove(key);
    }
}
