package org.sokybot.party.projections.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.sokybot.party.api.IPartySnapshot;
import org.sokybot.gameevents.events.party.PartyMatchingListEvent;

import reactor.core.publisher.Sinks;

final class MachinePartyState {

    volatile Integer selfEntityId;

    volatile int partyId;
    volatile int leaderEntityId;
    volatile long formedAtEpochMs;
    volatile boolean partyPvpEnabled;
    volatile boolean expShareEnabled;

    volatile long lastInviteEpochMs;

    final ConcurrentMap<Integer, TacticalPartyMember> members = new ConcurrentHashMap<>();

    volatile List<PartyMatchingListEvent.PartyMatchEntry> lastMatchingEntries = Collections.emptyList();

    final Sinks.Many<IPartySnapshot> snapshotSink = Sinks.many().multicast().onBackpressureBuffer(64, false);

    void replaceMatchingEntries(List<PartyMatchingListEvent.PartyMatchEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            this.lastMatchingEntries = Collections.emptyList();
        } else {
            this.lastMatchingEntries = Collections.unmodifiableList(new ArrayList<>(entries));
        }
    }
}
