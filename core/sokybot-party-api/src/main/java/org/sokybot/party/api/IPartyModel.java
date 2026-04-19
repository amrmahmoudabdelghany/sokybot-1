package org.sokybot.party.api;

import java.util.Optional;

import reactor.core.publisher.Flux;

/**
 * OSGi service: reactive party overlay per bot (machine full name {@code group.machineName}).
 */
public interface IPartyModel {

    /**
     * Latest immutable snapshot for the machine, if any party state exists.
     */
    Optional<IPartySnapshot> snapshot(String machineFullName);

    /**
     * Stream of snapshots while subscribed (implementations typically sample/push at a bounded rate).
     */
    Flux<IPartySnapshot> observe(String machineFullName);
}
