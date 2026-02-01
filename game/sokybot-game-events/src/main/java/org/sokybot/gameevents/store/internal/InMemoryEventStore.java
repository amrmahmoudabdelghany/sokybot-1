package org.sokybot.gameevents.store.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.store.IEventStore;

/**
 * In-memory implementation of IEventStore using a ring buffer.
 * Automatically subscribes to all OSGi game events.
 */
@Component(service = { IEventStore.class, EventHandler.class }, property = {
        EventConstants.EVENT_TOPIC + "=sokybot/game/*"
}, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class InMemoryEventStore implements IEventStore, EventHandler {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventStore.class);
    private static final int MAX_EVENTS_PER_MACHINE = 1000;

    private final Map<String, Deque<IGameEvent>> store = new ConcurrentHashMap<>();

    @Override
    public void handleEvent(Event event) {
        Object property = event.getProperty("event");
        if (property instanceof IGameEvent) {
            store((IGameEvent) property);
        }
    }

    @Override
    public void store(IGameEvent event) {
        String machineId = event.getFullName();
        if (machineId == null || machineId.isEmpty()) {
            return;
        }

        store.compute(machineId, (k, deque) -> {
            if (deque == null) {
                deque = new ConcurrentLinkedDeque<>();
            }

            deque.addFirst(event);

            // Maintain size limit
            while (deque.size() > MAX_EVENTS_PER_MACHINE) {
                deque.removeLast();
            }

            return deque;
        });
    }

    @Override
    public List<IGameEvent> getRecentEvents(String machineId, int limit) {
        Deque<IGameEvent> deque = store.get(machineId);
        if (deque == null) {
            return Collections.emptyList();
        }

        return deque.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<IGameEvent> getEvents(String machineId, Instant start, Instant end) {
        Deque<IGameEvent> deque = store.get(machineId);
        if (deque == null) {
            return Collections.emptyList();
        }

        long startMillis = start.toEpochMilli();
        long endMillis = end.toEpochMilli();

        return deque.stream()
                .filter(e -> e.getTimestamp() >= startMillis && e.getTimestamp() < endMillis)
                .sorted(Comparator.comparingLong(IGameEvent::getTimestamp))
                .collect(Collectors.toList());
    }

    @Override
    public void clear(String machineId) {
        store.remove(machineId);
    }

    @Override
    public int getEventCount(String machineId) {
        Deque<IGameEvent> deque = store.get(machineId);
        return deque != null ? deque.size() : 0;
    }

    @Override
    public List<String> getMachineIds() {
        return new ArrayList<>(store.keySet());
    }
}
