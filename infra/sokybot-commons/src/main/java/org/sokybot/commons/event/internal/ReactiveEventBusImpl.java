package org.sokybot.commons.event.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.commons.event.IReactiveEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * In-process reactive event bus. Game events are delivered via {@link #publish(Object)} from the
 * code paths that also post to Event Admin ({@code sokybot/game/...}), because many Event Admin
 * implementations match {@code *} as a single topic segment and do not deliver
 * {@code sokybot/game/<machine>/<EventType>} to a handler registered as
 * {@code sokybot/game/*}.
 */
@Component(service = IReactiveEventBus.class)
public class ReactiveEventBusImpl implements IReactiveEventBus {

    private static final Logger log = LoggerFactory.getLogger(ReactiveEventBusImpl.class);

    private final Sinks.Many<Object> sink;
    private final Flux<Object> flux;

    public ReactiveEventBusImpl() {
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
        this.flux = this.sink.asFlux().publish().autoConnect();
    }

    @Override
    public <T> Flux<T> on(Class<T> eventType) {
        return flux.ofType(eventType);
    }

    @Override
    public void publish(Object event) {
        if (event == null)
            return;

        sink.tryEmitNext(event).orThrow();
        log.trace("Published event to reactive bus: {}", event.getClass().getSimpleName());
    }
}
