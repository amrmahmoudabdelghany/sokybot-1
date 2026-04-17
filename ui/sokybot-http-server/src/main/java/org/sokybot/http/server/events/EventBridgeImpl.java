package org.sokybot.http.server.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.sokybot.commons.topic.Topic;
import org.sokybot.commons.topic.TopicMatcher;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

/**
 * Implementation of the event bridge using in-memory pub/sub.
 */
@Component(service = IEventBridge.class)
public class EventBridgeImpl implements IEventBridge {

    private static final Logger log = LoggerFactory.getLogger(EventBridgeImpl.class);

    private final List<SubscriptionImpl> subscriptions = new CopyOnWriteArrayList<>();
    private final AtomicLong eventCount = new AtomicLong(0);
    private final AtomicLong relayDroppedCount = new AtomicLong(0);
    private final Sinks.Many<BridgeEvent<Object>> relaySink = Sinks.many().multicast()
            .onBackpressureBuffer(8192, false);
    private Disposable relaySubscription;

    @Activate
    void activate() {
        relaySubscription = relaySink.asFlux().subscribe(this::deliverToSubscribers,
                error -> log.warn("EventBridge relay sink terminated", error));
    }

    @Deactivate
    void deactivate() {
        if (relaySubscription != null) {
            relaySubscription.dispose();
        }
    }

    @Override
    public <T> void publish(Topic topic, T event) {
        publish(null, topic, event);
    }

    @Override
    public <T> void publish(String machineId, Topic topic, T event) {
        eventCount.incrementAndGet();
        BridgeEvent<Object> bridgeEvent = new BridgeEvent<>(topic, machineId, event);
        Sinks.EmitResult result = relaySink.tryEmitNext(bridgeEvent);
        if (result.isFailure() && result != Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
            relayDroppedCount.incrementAndGet();
            log.debug("Dropping bridge event {} due to {}", bridgeEvent.getTopic(), result);
        }
    }

    @Override
    public <T> Subscription subscribe(Topic pattern, Consumer<BridgeEvent<T>> callback) {
        return subscribe(null, pattern, null, callback);
    }

    @Override
    public <T> Subscription subscribe(Topic pattern, Class<T> payloadType, Consumer<BridgeEvent<T>> callback) {
        return subscribe(null, pattern, payloadType, callback);
    }

    @Override
    public <T> Subscription subscribe(String machineId, Topic pattern, Consumer<BridgeEvent<T>> callback) {
        return subscribe(machineId, pattern, null, callback);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Subscription subscribe(String machineId, Topic pattern, Class<T> payloadType, Consumer<BridgeEvent<T>> callback) {
        SubscriptionImpl sub = new SubscriptionImpl(machineId, pattern, payloadType, (Consumer<BridgeEvent<Object>>) (Consumer<?>) callback);
        subscriptions.add(sub);
        log.debug("New subscription: pattern={}, machineId={}, payloadType={}", pattern, machineId, payloadType);
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

    /** Relay buffer drops (overflow / backpressure) since startup. */
    public long getRelayDroppedCount() {
        return relayDroppedCount.get();
    }

    /**
     * Internal subscription implementation.
     */
    private class SubscriptionImpl implements Subscription {
        final String machineIdFilter;
        final Topic pattern;
        final Class<?> payloadType;
        final Consumer<BridgeEvent<Object>> callback;
        final AtomicBoolean active = new AtomicBoolean(true);

        SubscriptionImpl(String machineIdFilter, Topic pattern, Class<?> payloadType, Consumer<BridgeEvent<Object>> callback) {
            this.machineIdFilter = machineIdFilter;
            this.pattern = pattern;
            this.payloadType = payloadType;
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
    private void deliverToSubscribers(BridgeEvent<Object> bridgeEvent) {
        for (SubscriptionImpl sub : subscriptions) {
            if (!sub.isActive()) continue;
            if (sub.machineIdFilter != null && bridgeEvent.getMachineId() != null
                    && !sub.machineIdFilter.equals(bridgeEvent.getMachineId())) continue;
            if (sub.machineIdFilter != null && bridgeEvent.getMachineId() == null) continue;
            if (!TopicMatcher.DEFAULT.matches(sub.pattern, bridgeEvent.getTopicObj())) continue;
            if (sub.payloadType != null && !sub.payloadType.isInstance(bridgeEvent.getPayload())) continue;
            try {
                sub.callback.accept(bridgeEvent);
            } catch (Exception e) {
                log.warn("Error delivering event to subscriber: {}", e.getMessage(), e);
            }
        }
    }
}
