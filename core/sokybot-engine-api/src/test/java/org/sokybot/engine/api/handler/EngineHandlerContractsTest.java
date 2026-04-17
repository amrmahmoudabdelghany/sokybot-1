package org.sokybot.engine.api.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.sokybot.engine.api.EngineEvent;
import org.sokybot.engine.api.event.Connect;

class EngineHandlerContractsTest {

    @Test
    void handlerShouldExposeEventTypeAndDefaultRanking() {
        IEngineEventHandler<Connect> handler = new IEngineEventHandler<Connect>() {
            @Override
            public Class<Connect> eventType() {
                return Connect.class;
            }

            @Override
            public void handle(Connect event, IEngineRuntime runtime) {
                runtime.publishLifecycle("CONNECT");
            }
        };

        assertEquals(Connect.class, handler.eventType());
        assertEquals(0, handler.getRanking());
    }

    @Test
    void mediatorSubscriptionShouldBeCloseable() {
        AtomicReference<EngineEvent> captured = new AtomicReference<>();
        IEngineEventMediator mediator = new IEngineEventMediator() {
            @Override
            public <E extends EngineEvent> void relay(E event) {
                captured.set(event);
            }

            @Override
            public <E extends EngineEvent> Subscription subscribe(Class<E> eventType,
                    java.util.function.Consumer<E> consumer) {
                return () -> {
                };
            }
        };

        IEngineEventMediator.Subscription subscription = mediator.subscribe(Connect.class, event -> {
            captured.set(event);
        });

        assertNotNull(subscription);
        mediator.relay(Connect.INSTANCE);
        assertEquals(Connect.INSTANCE, captured.get());
    }
}
