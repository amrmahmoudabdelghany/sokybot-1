package org.sokybot.trade.projections.internal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.trade.events.StallClosed;
import org.sokybot.trade.events.StallItemListed;
import org.sokybot.trade.events.StallItemSold;
import org.sokybot.trade.events.StallOpened;
import org.sokybot.trade.projections.api.IStallModel;
import org.sokybot.trade.projections.api.StallSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;

@Component(service = IStallModel.class, immediate = true)
public final class StallModelComponent implements IStallModel {

    private static final Logger log = LoggerFactory.getLogger(StallModelComponent.class);

    private final Map<String, StallState> stateByMachine = new ConcurrentHashMap<>();

    @Reference
    private IReactiveEventBus reactiveEventBus;

    private Disposable d1;
    private Disposable d2;
    private Disposable d3;
    private Disposable d4;

    @Activate
    void activate() {
        d1 = reactiveEventBus.on(StallOpened.class).subscribe(this::onOpened);
        d2 = reactiveEventBus.on(StallItemListed.class).subscribe(this::onListed);
        d3 = reactiveEventBus.on(StallItemSold.class).subscribe(this::onSold);
        d4 = reactiveEventBus.on(StallClosed.class).subscribe(this::onClosed);
        log.debug("IStallModel projection active");
    }

    @Deactivate
    void deactivate() {
        disposeQuietly(d1);
        disposeQuietly(d2);
        disposeQuietly(d3);
        disposeQuietly(d4);
        stateByMachine.clear();
    }

    private static void disposeQuietly(Disposable d) {
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private void onOpened(StallOpened e) {
        String key = e.getFullName();
        stateByMachine.compute(key, (k, v) -> {
            StallState s = v != null ? v : new StallState();
            s.open = true;
            s.entityId = e.getStallEntityId();
            s.title = e.getStallTitle();
            return s;
        });
    }

    private void onListed(StallItemListed e) {
        String key = e.getFullName();
        stateByMachine.computeIfPresent(key, (k, s) -> {
            s.entityId = e.getStallEntityId();
            return s;
        });
    }

    private void onSold(StallItemSold e) {
        String key = e.getFullName();
        stateByMachine.compute(key, (k, v) -> {
            StallState s = v != null ? v : new StallState();
            s.open = true;
            s.entityId = e.getStallEntityId();
            s.lastSale = e;
            return s;
        });
    }

    private void onClosed(StallClosed e) {
        stateByMachine.remove(e.getFullName());
    }

    @Override
    public Optional<StallSnapshot> getSnapshot(String machineFullName) {
        if (machineFullName == null) {
            return Optional.empty();
        }
        StallState s = stateByMachine.get(machineFullName.trim());
        if (s == null || !s.open) {
            return Optional.empty();
        }
        return Optional.of(new StallSnapshot(machineFullName, true, s.entityId, s.title, s.lastSale));
    }

    private static final class StallState {
        boolean open;
        int entityId = -1;
        String title = "";
        StallItemSold lastSale;
    }
}
