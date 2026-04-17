package org.sokybot.commons.osgi;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Snapshot-friendly service handle for dynamic OSGi references.
 */
public interface ServiceHandle<T> {
    Optional<T> tryGet();

    default void ifPresent(Consumer<T> consumer) {
        tryGet().ifPresent(consumer);
    }

    default <R> Optional<R> map(Function<T, R> mapper) {
        return tryGet().map(mapper);
    }

    default boolean isAvailable() {
        return tryGet().isPresent();
    }
}
