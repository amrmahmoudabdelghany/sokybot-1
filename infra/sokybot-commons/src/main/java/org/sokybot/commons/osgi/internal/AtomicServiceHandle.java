package org.sokybot.commons.osgi.internal;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.sokybot.commons.osgi.ServiceHandle;

public final class AtomicServiceHandle<T> implements ServiceHandle<T> {
    private final AtomicReference<T> reference = new AtomicReference<>();

    @Override
    public void bind(T service) {
        reference.set(service);
    }

    @Override
    public void unbind(T service) {
        reference.compareAndSet(service, null);
    }

    @Override
    public Optional<T> tryGet() {
        return Optional.ofNullable(reference.get());
    }
}
