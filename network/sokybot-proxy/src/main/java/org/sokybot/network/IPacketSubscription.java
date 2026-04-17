package org.sokybot.network;

import org.sokybot.commons.lifecycle.Subscription;

public interface IPacketSubscription extends Subscription {
    void unsubscribe();

    @Override
    default void close() {
        unsubscribe();
    }
}
