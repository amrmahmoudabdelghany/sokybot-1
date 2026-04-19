package org.sokybot.party.api;

import java.util.Collections;
import java.util.List;

/**
 * Immutable read-only view of party state for one machine (character scope).
 */
public interface IPartySnapshot {

    /** Machine full name ({@code group.machineName}). */
    String getMachineFullName();

    /** Server party identifier when known; non-positive when absent or unknown. */
    int getPartyId();

    /** Unique entity id of the party leader when known; non-positive if unknown. */
    int getLeaderEntityId();

    /** Members keyed by projection order (leader first when deterministically sorted). */
    List<PartyMember> getMembers();

    /** Epoch millis when this party roster was first observed for this session segment. */
    long getFormedAtEpochMs();

    /** Party PvP / friendly-fire style flag when exposed by server (best-effort). */
    boolean isPartyPvpEnabled();

    /** Experience sharing enabled within party. */
    boolean isExpShareEnabled();

    /** Epoch millis of the last observed {@code PartyInviteEvent} for this machine; {@code 0} if none. */
    default long getLastInviteEpochMs() {
        return 0L;
    }

    /** Last cached party-matching page (empty when unknown). */
    default List<PartyMatchListing> getMatchingListings() {
        return Collections.emptyList();
    }
}
