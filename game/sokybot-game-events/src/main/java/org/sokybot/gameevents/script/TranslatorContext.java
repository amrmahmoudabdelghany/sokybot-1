package org.sokybot.gameevents.script;

import java.util.Collections;
import java.util.List;

import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.ChunkedPacketManagerRegistry;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Delegate object for Groovy translator closures. Exposes lookup, chunk manager,
 * and helper methods so scripts can use them without changing the closure signature.
 */
public class TranslatorContext {

    public final IGameDataLookup lookup;

    public TranslatorContext(IGameDataLookup lookup) {
        this.lookup = lookup;
    }

    public List<IGameEvent> singleEvent(IGameEvent event) {
        return event != null ? List.of(event) : Collections.emptyList();
    }

    public List<IGameEvent> noEvents() {
        return Collections.emptyList();
    }

    public ChunkedPacketManager getChunkManager(String machineFullName) {
        return ChunkedPacketManagerRegistry.getInstance().get(machineFullName);
    }
}
