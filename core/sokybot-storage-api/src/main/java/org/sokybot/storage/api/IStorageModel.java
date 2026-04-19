package org.sokybot.storage.api;

import java.util.Optional;

import reactor.core.publisher.Flux;

/**
 * Reactive storage overlay per bot (machine full name {@code group.machineName}).
 */
public interface IStorageModel {

    Optional<IStorageSnapshot> personal(String machineFullName);

    Optional<IGuildStorageSnapshot> guild(String machineFullName);

    Flux<IStorageSnapshot> observePersonal(String machineFullName);

    Flux<IGuildStorageSnapshot> observeGuild(String machineFullName);

    /**
     * True when the most-recent open ack is within {@code maxAgeMs} for the given storage kind.
     */
    boolean isStorageFresh(String machineFullName, StorageType type, long maxAgeMs);
}
