package org.sokybot.gamemodel;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gamemodel.internal.DefaultEntityFactory;
import org.sokybot.gamemodel.internal.GameModelImpl;
import org.sokybot.gamemodel.model.ISpawn;

import reactor.core.publisher.Flux;

class GameModelImplSnapshotTest {

    @Test
    void snapshotAllShouldReturnUnmodifiableList() {
        IReactiveEventBus bus = new IReactiveEventBus() {
            @Override
            public <T> Flux<T> on(Class<T> eventType) {
                return Flux.empty();
            }

            @Override
            public void publish(Object event) {
            }
        };
        GameModelImpl model = new GameModelImpl("g.m", bus, new DefaultEntityFactory());
        List<ISpawn> snapshots = model.snapshotAll(ISpawn.class);
        assertTrue(!snapshots.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> snapshots.add(null));
    }
}
