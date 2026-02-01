package org.sokybot.commons.event.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sokybot.commons.event.IReactiveEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Implementation of IReactiveEventBus.
 * Bridges OSGi Events to Reactor Flux stream.
 */
@Component(service = { IReactiveEventBus.class, EventHandler.class }, property = {
        EventConstants.EVENT_TOPIC + "=sokybot/game/*"
})
public class ReactiveEventBusImpl implements IReactiveEventBus, EventHandler {

    private static final Logger log = LoggerFactory.getLogger(ReactiveEventBusImpl.class);

    private final Sinks.Many<Object> sink;
    private final Flux<Object> flux;

    public ReactiveEventBusImpl() {
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
        this.flux = this.sink.asFlux().publish().autoConnect();
    }

    @Override
    public void handleEvent(Event event) {
        Object eventObj = event.getProperty("event");
        if (eventObj != null) {
            publish(eventObj);
        }
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
