package org.sokybot.http.server.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.sokybot.commons.topic.Topic;

class EventBridgeImplTest {
    @Test
    void shouldDeliverTypedEventsAndFilterByClass() {
        EventBridgeImpl bridge = new EventBridgeImpl();
        bridge.activate();
        try {
            AtomicInteger allEvents = new AtomicInteger();
            AtomicInteger mapEvents = new AtomicInteger();
            bridge.subscribe(Topic.parse("sokybot.game.**"), e -> allEvents.incrementAndGet());
            bridge.subscribe(Topic.parse("sokybot.game.**"), java.util.Map.class, e -> mapEvents.incrementAndGet());
            bridge.publish(Topic.parse("sokybot.game.a.b"), "string");
            bridge.publish(Topic.parse("sokybot.game.a.b"), java.util.Map.of("k", "v"));
            assertEquals(2, allEvents.get());
            assertEquals(1, mapEvents.get());
        } finally {
            bridge.deactivate();
        }
    }

    @Test
    void shouldKeepDeprecatedStringMethodsWorking() {
        EventBridgeImpl bridge = new EventBridgeImpl();
        bridge.activate();
        try {
            AtomicInteger count = new AtomicInteger();
            IEventBridge.Subscription sub = bridge.subscribe("sokybot.game.**", e -> count.incrementAndGet());
            bridge.publish("sokybot.game.machine.Event", "payload");
            assertEquals(1, count.get());
            assertTrue(sub.isActive());
            sub.unsubscribe();
            assertFalse(sub.isActive());
        } finally {
            bridge.deactivate();
        }
    }
}
