package org.sokybot.commons.osgi;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic implementation for dynamic service bind/unbind.
 */
public final class AtomicServiceHandle<T> implements ServiceHandle<T> {
    private final AtomicReference<T> reference = new AtomicReference<>();

    public void set(T service) {
        reference.set(service);
    }

    public void clear(T service) {
        reference.compareAndSet(service, null);
    }

    @Override
    public Optional<T> tryGet() {
        return Optional.ofNullable(reference.get());
    }
}
