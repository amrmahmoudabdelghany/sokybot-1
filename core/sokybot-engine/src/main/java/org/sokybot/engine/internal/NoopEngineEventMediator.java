package org.sokybot.engine.internal;

import java.util.function.Consumer;

import org.sokybot.engine.api.EngineEvent;
import org.sokybot.engine.api.handler.IEngineEventMediator;

public final class NoopEngineEventMediator implements IEngineEventMediator {

    public static final IEngineEventMediator INSTANCE = new NoopEngineEventMediator();

    private static final Subscription NOOP_SUBSCRIPTION = () -> {
        // no-op
    };

    private NoopEngineEventMediator() {
    }

    @Override
    public <E extends EngineEvent> void relay(E event) {
        // no-op
    }

    @Override
    public <E extends EngineEvent> Subscription subscribe(Class<E> eventType, Consumer<E> consumer) {
        return NOOP_SUBSCRIPTION;
    }
}
