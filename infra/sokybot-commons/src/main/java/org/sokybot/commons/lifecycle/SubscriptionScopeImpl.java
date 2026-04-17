package org.sokybot.commons.lifecycle;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SubscriptionScopeImpl implements ISubscriptionScope {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionScopeImpl.class);

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final List<AutoCloseable> resources = new CopyOnWriteArrayList<>();

    @Override
    public <S extends Subscription> S register(S subscription) {
        register((AutoCloseable) subscription);
        return subscription;
    }

    @Override
    public void register(AutoCloseable resource) {
        if (resource == null) {
            return;
        }
        if (closed.get()) {
            throw new IllegalStateException("Subscription scope is already closed");
        }
        resources.add(resource);
    }

    @Override
    public int size() {
        return resources.size();
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        for (int i = resources.size() - 1; i >= 0; i--) {
            AutoCloseable resource = resources.get(i);
            try {
                resource.close();
            } catch (Exception e) {
                log.warn("Failed to close scoped resource {}", resource.getClass().getName(), e);
            }
        }
        resources.clear();
    }
}
