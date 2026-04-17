package org.sokybot.commons.lifecycle;

public interface ISubscriptionScope extends AutoCloseable {
    <S extends Subscription> S register(S subscription);

    void register(AutoCloseable resource);

    int size();

    boolean isClosed();

    @Override
    void close();
}
