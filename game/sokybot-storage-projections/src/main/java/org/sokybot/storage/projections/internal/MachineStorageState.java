package org.sokybot.storage.projections.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.sokybot.storage.api.IGuildStorageSnapshot;
import org.sokybot.storage.api.IStorageSnapshot;

import reactor.core.publisher.Sinks;

/**
 * Per-machine warehouse overlay (personal + guild maps and reactive sinks).
 */
final class MachineStorageState {

    final ConcurrentMap<Integer, StorageStackMutable> personalStacks = new ConcurrentHashMap<>();
    final ConcurrentMap<Integer, StorageStackMutable> guildStacks = new ConcurrentHashMap<>();

    volatile long personalGold;
    volatile long guildGold;

    volatile int personalTotalSlots;
    volatile int guildTotalSlots;

    volatile int personalFilledSlots = -1;
    volatile int guildFilledSlots = -1;

    volatile long lastPersonalOpenEpochMs;
    volatile long lastGuildOpenEpochMs;

    volatile boolean personalFresh;
    volatile boolean guildFresh;

    volatile long capturedAtEpochMs;

    final Sinks.Many<IStorageSnapshot> personalSink =
            Sinks.many().multicast().onBackpressureBuffer(64, false);

    final Sinks.Many<IGuildStorageSnapshot> guildSink =
            Sinks.many().multicast().onBackpressureBuffer(64, false);
}
