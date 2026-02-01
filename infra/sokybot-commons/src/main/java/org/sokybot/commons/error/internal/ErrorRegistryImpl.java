package org.sokybot.commons.error.internal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.sokybot.commons.error.ErrorInfo;
import org.sokybot.commons.error.IErrorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * OSGi DS implementation of IErrorRegistry.
 * 
 * Stores recent errors in a bounded queue and provides a reactive stream
 * for real-time error notifications.
 */
@Component(service = IErrorRegistry.class, immediate = true)
public class ErrorRegistryImpl implements IErrorRegistry {

    private static final Logger log = LoggerFactory.getLogger(ErrorRegistryImpl.class);

    /**
     * Maximum number of errors to store.
     */
    private static final int MAX_ERRORS = 100;

    /**
     * Bounded error storage (most recent at end).
     */
    private final LinkedList<ErrorInfo> errors = new LinkedList<>();

    /**
     * Reactive sink for streaming errors.
     */
    private final Sinks.Many<ErrorInfo> errorSink = Sinks.many().multicast().onBackpressureBuffer(50);

    @Activate
    protected void activate() {
        log.info("Error Registry activated");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Error Registry deactivating");
        errorSink.tryEmitComplete();
        synchronized (errors) {
            errors.clear();
        }
    }

    @Override
    public void recordError(ErrorInfo error) {
        if (error == null) {
            return;
        }

        log.warn("Error recorded: [{}] {} - {}",
                error.getCategory(), error.getSource(), error.getMessage());

        synchronized (errors) {
            errors.addFirst(error);

            // Trim to max size
            while (errors.size() > MAX_ERRORS) {
                errors.removeLast();
            }
        }

        // Emit to stream (non-blocking, ignores failure)
        Sinks.EmitResult result = errorSink.tryEmitNext(error);
        if (result.isFailure()) {
            log.debug("Failed to emit error to stream: {}", result);
        }
    }

    @Override
    public List<ErrorInfo> getRecentErrors(int limit) {
        synchronized (errors) {
            int size = Math.min(limit, errors.size());
            return new ArrayList<>(errors.subList(0, size));
        }
    }

    @Override
    public Flux<ErrorInfo> getErrorStream() {
        return errorSink.asFlux();
    }

    @Override
    public void clearErrors() {
        synchronized (errors) {
            errors.clear();
        }
        log.info("Error history cleared");
    }

    @Override
    public int getErrorCount() {
        synchronized (errors) {
            return errors.size();
        }
    }
}
