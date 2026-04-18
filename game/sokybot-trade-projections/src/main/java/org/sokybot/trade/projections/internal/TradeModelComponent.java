package org.sokybot.trade.projections.internal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.trade.events.TradeExchangeApproved;
import org.sokybot.trade.events.TradeExchangeCancelled;
import org.sokybot.trade.events.TradeItemAdded;
import org.sokybot.trade.events.TradePartnerConfirmed;
import org.sokybot.trade.events.TradeWindowOpened;
import org.sokybot.trade.projections.api.ITradeModel;
import org.sokybot.trade.projections.api.TradeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;

/**
 * Subscribes to trade {@link org.sokybot.trade.events} types on the reactive bus.
 */
@Component(service = ITradeModel.class, immediate = true)
public final class TradeModelComponent implements ITradeModel {

    private static final Logger log = LoggerFactory.getLogger(TradeModelComponent.class);

    private final Map<String, TradeState> stateByMachine = new ConcurrentHashMap<>();

    @Reference
    private IReactiveEventBus reactiveEventBus;

    private Disposable d1;
    private Disposable d2;
    private Disposable d3;
    private Disposable d4;
    private Disposable d5;

    @Activate
    void activate() {
        d1 = reactiveEventBus.on(TradeWindowOpened.class).subscribe(this::onOpened);
        d2 = reactiveEventBus.on(TradeItemAdded.class).subscribe(this::onItem);
        d3 = reactiveEventBus.on(TradeExchangeCancelled.class).subscribe(this::onCancelled);
        d4 = reactiveEventBus.on(TradeExchangeApproved.class).subscribe(this::onApproved);
        d5 = reactiveEventBus.on(TradePartnerConfirmed.class).subscribe(this::onConfirmed);
        log.debug("ITradeModel projection active");
    }

    @Deactivate
    void deactivate() {
        disposeQuietly(d1);
        disposeQuietly(d2);
        disposeQuietly(d3);
        disposeQuietly(d4);
        disposeQuietly(d5);
        stateByMachine.clear();
    }

    private static void disposeQuietly(Disposable d) {
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private void onOpened(TradeWindowOpened e) {
        String key = e.getFullName();
        stateByMachine.compute(key, (k, v) -> {
            TradeState s = v != null ? v : new TradeState();
            s.active = true;
            s.otherPlayerUniqueId = e.getOtherPlayerUniqueId();
            s.exchangeId = -1;
            return s;
        });
    }

    private void onItem(TradeItemAdded e) {
        String key = e.getFullName();
        stateByMachine.compute(key, (k, v) -> {
            TradeState s = v != null ? v : new TradeState();
            s.active = true;
            s.exchangeId = e.getExchangeId();
            s.lastSlotUpdate = e;
            return s;
        });
    }

    private void onConfirmed(TradePartnerConfirmed e) {
        String key = e.getFullName();
        stateByMachine.computeIfPresent(key, (k, s) -> {
            s.exchangeId = e.getExchangeId();
            return s;
        });
    }

    private void onCancelled(TradeExchangeCancelled e) {
        stateByMachine.remove(e.getFullName());
    }

    private void onApproved(TradeExchangeApproved e) {
        stateByMachine.remove(e.getFullName());
    }

    @Override
    public Optional<TradeSnapshot> getSnapshot(String machineFullName) {
        if (machineFullName == null) {
            return Optional.empty();
        }
        TradeState s = stateByMachine.get(machineFullName.trim());
        if (s == null || !s.active) {
            return Optional.empty();
        }
        return Optional.of(new TradeSnapshot(machineFullName, s.active, s.otherPlayerUniqueId, s.exchangeId,
                s.lastSlotUpdate));
    }

    private static final class TradeState {
        boolean active;
        int otherPlayerUniqueId = -1;
        int exchangeId = -1;
        TradeItemAdded lastSlotUpdate;
    }
}
