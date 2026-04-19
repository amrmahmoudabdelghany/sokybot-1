package org.sokybot.party.projections.internal;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.sokybot.party.api.IPartySnapshot;
import org.sokybot.party.api.PartyMatchListing;
import org.sokybot.party.api.PartyMember;

final class PartySnapshot implements IPartySnapshot {

    private final String machineFullName;
    private final int partyId;
    private final int leaderEntityId;
    private final List<PartyMember> members;
    private final long formedAtEpochMs;
    private final boolean partyPvpEnabled;
    private final boolean expShareEnabled;
    private final long lastInviteEpochMs;
    private final List<PartyMatchListing> matchingListings;

    private PartySnapshot(Builder b) {
        this.machineFullName = Objects.requireNonNull(b.machineFullName, "machineFullName");
        this.partyId = b.partyId;
        this.leaderEntityId = b.leaderEntityId;
        this.members = Objects.requireNonNull(b.members, "members");
        this.formedAtEpochMs = b.formedAtEpochMs;
        this.partyPvpEnabled = b.partyPvpEnabled;
        this.expShareEnabled = b.expShareEnabled;
        this.lastInviteEpochMs = b.lastInviteEpochMs;
        this.matchingListings = Objects.requireNonNull(b.matchingListings, "matchingListings");
    }

    @Override
    public String getMachineFullName() {
        return machineFullName;
    }

    @Override
    public int getPartyId() {
        return partyId;
    }

    @Override
    public int getLeaderEntityId() {
        return leaderEntityId;
    }

    @Override
    public List<PartyMember> getMembers() {
        return members;
    }

    @Override
    public long getFormedAtEpochMs() {
        return formedAtEpochMs;
    }

    @Override
    public boolean isPartyPvpEnabled() {
        return partyPvpEnabled;
    }

    @Override
    public boolean isExpShareEnabled() {
        return expShareEnabled;
    }

    @Override
    public long getLastInviteEpochMs() {
        return lastInviteEpochMs;
    }

    @Override
    public List<PartyMatchListing> getMatchingListings() {
        return matchingListings;
    }

    static Builder builder(String machineFullName) {
        return new Builder(machineFullName);
    }

    static final class Builder {
        private final String machineFullName;
        private int partyId;
        private int leaderEntityId;
        private List<PartyMember> members;
        private long formedAtEpochMs;
        private boolean partyPvpEnabled;
        private boolean expShareEnabled;
        private long lastInviteEpochMs;
        private List<PartyMatchListing> matchingListings = Collections.emptyList();

        private Builder(String machineFullName) {
            this.machineFullName = machineFullName;
        }

        Builder partyId(int partyId) {
            this.partyId = partyId;
            return this;
        }

        Builder leaderEntityId(int leaderEntityId) {
            this.leaderEntityId = leaderEntityId;
            return this;
        }

        Builder members(List<PartyMember> members) {
            this.members = members;
            return this;
        }

        Builder formedAtEpochMs(long formedAtEpochMs) {
            this.formedAtEpochMs = formedAtEpochMs;
            return this;
        }

        Builder partyPvpEnabled(boolean partyPvpEnabled) {
            this.partyPvpEnabled = partyPvpEnabled;
            return this;
        }

        Builder expShareEnabled(boolean expShareEnabled) {
            this.expShareEnabled = expShareEnabled;
            return this;
        }

        Builder lastInviteEpochMs(long lastInviteEpochMs) {
            this.lastInviteEpochMs = lastInviteEpochMs;
            return this;
        }

        Builder matchingListings(List<PartyMatchListing> matchingListings) {
            this.matchingListings = matchingListings;
            return this;
        }

        PartySnapshot build() {
            return new PartySnapshot(this);
        }
    }
}
