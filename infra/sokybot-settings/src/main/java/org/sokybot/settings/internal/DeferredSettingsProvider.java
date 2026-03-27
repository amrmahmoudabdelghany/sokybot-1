package org.sokybot.settings.internal;

import lombok.extern.slf4j.Slf4j;
import org.sokybot.settings.api.ISettingsProvider;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Lazy settings provider that can be returned before a scope is registered.
 * It binds to a concrete provider once the corresponding scope registration appears.
 */
@Slf4j
class DeferredSettingsProvider<T> implements ISettingsProvider<T> {

    private final String scope;
    private final String providerKey;
    private volatile ISettingsProvider<T> delegate;
    private final List<Consumer<T>> pendingSubscribers = new CopyOnWriteArrayList<>();

    DeferredSettingsProvider(String scope, String providerKey) {
        this.scope = scope;
        this.providerKey = providerKey;
    }

    void bind(ISettingsProvider<T> realProvider) {
        if (realProvider == null) {
            return;
        }
        if (delegate != null) {
            return;
        }
        synchronized (this) {
            if (delegate != null) {
                return;
            }
            delegate = realProvider;
            List<Consumer<T>> queued = List.copyOf(pendingSubscribers);
            for (Consumer<T> subscriber : queued) {
                delegate.subscribe(subscriber);
            }
            pendingSubscribers.clear();
            try {
                T current = delegate.get();
                for (Consumer<T> subscriber : queued) {
                    subscriber.accept(current);
                }
            } catch (Exception e) {
                log.warn("Failed to notify deferred subscribers for scope '{}' ({})", scope, providerKey, e);
            }
        }
    }

    @Override
    public T get() {
        ISettingsProvider<T> current = delegate;
        return current != null ? current.get() : null;
    }

    @Override
    public void update(Consumer<T> mutator) {
        ISettingsProvider<T> current = delegate;
        if (current != null) {
            current.update(mutator);
            return;
        }
        log.warn("Skipping settings update; scope '{}' not registered yet ({})", scope, providerKey);
    }

    @Override
    public void save() {
        ISettingsProvider<T> current = delegate;
        if (current != null) {
            current.save();
            return;
        }
        log.warn("Skipping settings save; scope '{}' not registered yet ({})", scope, providerKey);
    }

    @Override
    public void reload() {
        ISettingsProvider<T> current = delegate;
        if (current != null) {
            current.reload();
            return;
        }
        log.warn("Skipping settings reload; scope '{}' not registered yet ({})", scope, providerKey);
    }

    @Override
    public void resetToDefaults() {
        ISettingsProvider<T> current = delegate;
        if (current != null) {
            current.resetToDefaults();
            return;
        }
        log.warn("Skipping settings resetToDefaults; scope '{}' not registered yet ({})", scope, providerKey);
    }

    @Override
    public boolean isDirty() {
        ISettingsProvider<T> current = delegate;
        return current != null && current.isDirty();
    }

    @Override
    public void subscribe(Consumer<T> listener) {
        ISettingsProvider<T> current = delegate;
        if (current != null) {
            current.subscribe(listener);
            return;
        }

        pendingSubscribers.add(listener);

        ISettingsProvider<T> afterAdd = delegate;
        if (afterAdd != null && pendingSubscribers.remove(listener)) {
            afterAdd.subscribe(listener);
        }
    }
}
