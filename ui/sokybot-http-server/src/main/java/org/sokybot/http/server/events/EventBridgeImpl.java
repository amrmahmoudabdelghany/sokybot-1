package org.sokybot.http.server.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the event bridge using in-memory pub/sub.
 */
@Component(service = IEventBridge.class)
public class EventBridgeImpl implements IEventBridge {

    private static final Logger log = LoggerFactory.getLogger(EventBridgeImpl.class);

    private final List<SubscriptionImpl> subscriptions = new CopyOnWriteArrayList<>();
    private final AtomicLong eventCount = new AtomicLong(0);

    @Override
    public void publish(String topic, Object event) {
        publish(null, topic, event);
    }

    @Override
    public void publish(String machineId, String topic, Object event) {
        eventCount.incrementAndGet();

        BridgeEvent bridgeEvent = new BridgeEvent(topic, machineId, event);

        log.debug("Publishing event: {}", bridgeEvent);

        for (SubscriptionImpl sub : subscriptions) {
            if (!sub.isActive()) {
                continue;
            }

            // Check machine ID filter
            if (sub.machineIdFilter != null && machineId != null
                    && !sub.machineIdFilter.equals(machineId)) {
                continue;
            }

            // Check topic pattern
            if (!bridgeEvent.matchesTopic(sub.pattern)) {
                continue;
            }

            // Deliver event
            try {
                sub.callback.accept(bridgeEvent);
            } catch (Exception e) {
                log.warn("Error delivering event to subscriber: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public Subscription subscribe(String pattern, Consumer<BridgeEvent> callback) {
        return subscribe(null, pattern, callback);
    }

    @Override
    public Subscription subscribe(String machineId, String pattern, Consumer<BridgeEvent> callback) {
        SubscriptionImpl sub = new SubscriptionImpl(machineId, pattern, callback);
        subscriptions.add(sub);
        log.debug("New subscription: pattern={}, machineId={}", pattern, machineId);
        return sub;
    }

    @Override
    public int getSubscriberCount() {
        return (int) subscriptions.stream().filter(SubscriptionImpl::isActive).count();
    }

    @Override
    public long getEventCount() {
        return eventCount.get();
    }

    /**
     * Internal subscription implementation.
     */
    private class SubscriptionImpl implements Subscription {
        final String machineIdFilter;
        final String pattern;
        final Consumer<BridgeEvent> callback;
        final AtomicBoolean active = new AtomicBoolean(true);

        SubscriptionImpl(String machineIdFilter, String pattern, Consumer<BridgeEvent> callback) {
            this.machineIdFilter = machineIdFilter;
            this.pattern = pattern;
            this.callback = callback;
        }

        @Override
        public void unsubscribe() {
            if (active.compareAndSet(true, false)) {
                subscriptions.remove(this);
                log.debug("Unsubscribed: pattern={}", pattern);
            }
        }

        @Override
        public boolean isActive() {
            return active.get();
        }
    }
}
