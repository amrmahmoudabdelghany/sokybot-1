package org.sokybot.party.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable {@link IPartyPolicy} implementation for derived strategies.
 */
public final class PartyPolicy implements IPartyPolicy {

    private final boolean partyCycleEnabled;
    private final boolean autoAcceptInvites;
    private final List<String> partyInviteWhitelist;
    private final boolean autoCreateMatch;
    private final boolean autoInviteFromMatching;
    private final String matchingTitle;
    private final boolean healOthers;
    private final int healHpThresholdPercent;
    private final int healSkillRefId;
    private final boolean followLeader;
    private final float followDistanceWorld;
    private final List<Integer> buffShareSkillIds;

    private PartyPolicy(Builder b) {
        this.partyCycleEnabled = b.partyCycleEnabled;
        this.autoAcceptInvites = b.autoAcceptInvites;
        this.partyInviteWhitelist = Collections.unmodifiableList(new ArrayList<>(b.partyInviteWhitelist));
        this.autoCreateMatch = b.autoCreateMatch;
        this.autoInviteFromMatching = b.autoInviteFromMatching;
        this.matchingTitle = b.matchingTitle != null ? b.matchingTitle : "";
        this.healOthers = b.healOthers;
        this.healHpThresholdPercent = b.healHpThresholdPercent;
        this.healSkillRefId = b.healSkillRefId;
        this.followLeader = b.followLeader;
        this.followDistanceWorld = b.followDistanceWorld;
        this.buffShareSkillIds = Collections.unmodifiableList(new ArrayList<>(b.buffShareSkillIds));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean isPartyCycleEnabled() {
        return partyCycleEnabled;
    }

    @Override
    public boolean isAutoAcceptInvites() {
        return autoAcceptInvites;
    }

    @Override
    public List<String> getPartyInviteWhitelist() {
        return partyInviteWhitelist;
    }

    @Override
    public boolean isAutoCreateMatch() {
        return autoCreateMatch;
    }

    @Override
    public boolean isAutoInviteFromMatching() {
        return autoInviteFromMatching;
    }

    @Override
    public String getMatchingTitle() {
        return matchingTitle;
    }

    @Override
    public boolean isHealOthers() {
        return healOthers;
    }

    @Override
    public int getHealHpThresholdPercent() {
        return healHpThresholdPercent;
    }

    @Override
    public int getHealSkillRefId() {
        return healSkillRefId;
    }

    @Override
    public boolean isFollowLeader() {
        return followLeader;
    }

    @Override
    public float getFollowDistanceWorld() {
        return followDistanceWorld;
    }

    @Override
    public List<Integer> getBuffShareSkillIds() {
        return buffShareSkillIds;
    }

    public static final class Builder {

        private boolean partyCycleEnabled;
        private boolean autoAcceptInvites;
        private final List<String> partyInviteWhitelist = new ArrayList<>();
        private boolean autoCreateMatch;
        private boolean autoInviteFromMatching;
        private String matchingTitle = "";
        private boolean healOthers;
        private int healHpThresholdPercent = 50;
        private int healSkillRefId = -1;
        private boolean followLeader;
        private float followDistanceWorld = 25.0f;
        private final List<Integer> buffShareSkillIds = new ArrayList<>();

        public Builder partyCycleEnabled(boolean v) {
            this.partyCycleEnabled = v;
            return this;
        }

        public Builder autoAcceptInvites(boolean v) {
            this.autoAcceptInvites = v;
            return this;
        }

        public Builder partyInviteWhitelist(List<String> names) {
            this.partyInviteWhitelist.clear();
            if (names != null) {
                for (String n : names) {
                    if (n != null && !n.trim().isEmpty()) {
                        this.partyInviteWhitelist.add(n.trim());
                    }
                }
            }
            return this;
        }

        public Builder autoCreateMatch(boolean v) {
            this.autoCreateMatch = v;
            return this;
        }

        public Builder autoInviteFromMatching(boolean v) {
            this.autoInviteFromMatching = v;
            return this;
        }

        public Builder matchingTitle(String v) {
            this.matchingTitle = v != null ? v : "";
            return this;
        }

        public Builder healOthers(boolean v) {
            this.healOthers = v;
            return this;
        }

        public Builder healHpThresholdPercent(int v) {
            this.healHpThresholdPercent = v;
            return this;
        }

        public Builder healSkillRefId(int v) {
            this.healSkillRefId = v;
            return this;
        }

        public Builder followLeader(boolean v) {
            this.followLeader = v;
            return this;
        }

        public Builder followDistanceWorld(float v) {
            this.followDistanceWorld = v;
            return this;
        }

        public Builder buffShareSkillIds(List<Integer> ids) {
            this.buffShareSkillIds.clear();
            if (ids != null) {
                this.buffShareSkillIds.addAll(ids);
            }
            return this;
        }

        public PartyPolicy build() {
            return new PartyPolicy(this);
        }
    }
}
