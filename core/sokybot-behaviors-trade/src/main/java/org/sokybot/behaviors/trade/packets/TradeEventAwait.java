package org.sokybot.behaviors.trade.packets;

import java.time.Duration;
import java.util.function.Predicate;

import org.sokybot.commons.event.IReactiveEventBus;

/**
 * Small blocking helpers for one-shot trade projection events (per workflow tick / machine thread).
 */
public final class TradeEventAwait {

    private TradeEventAwait() {
    }

    public static <T> T awaitNext(
            IReactiveEventBus bus,
            Class<T> type,
            Predicate<T> predicate,
            Duration timeout) {
        if (bus == null || type == null || predicate == null) {
            return null;
        }
        try {
            return bus.on(type)
                    .filter(predicate::test)
                    .next()
                    .timeout(timeout)
                    .blockOptional()
                    .orElse(null);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
