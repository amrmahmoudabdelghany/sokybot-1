package org.sokybot.commons.lifecycle;

/**
 * Lifecycle-safe subscription contract.
 */
public interface Subscription extends AutoCloseable {
    @Override
    void close();

    default boolean isActive() {
        return true;
    }

    default void unsubscribe() {
        close();
    }
}
