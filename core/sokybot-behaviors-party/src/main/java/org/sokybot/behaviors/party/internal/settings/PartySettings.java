package org.sokybot.behaviors.party.internal.settings;

import java.util.ArrayList;
import java.util.List;

import org.sokybot.party.api.IPartyPolicy;
import org.sokybot.party.api.IPartySettings;
import org.sokybot.party.api.PartyPolicy;

/**
 * Machine-scoped party automation settings (scope {@code party}).
 */
public final class PartySettings implements IPartySettings {

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

    @Override
    public boolean isPartyCycleEnabled() {
        return partyCycleEnabled;
    }

    public void setPartyCycleEnabled(boolean partyCycleEnabled) {
        this.partyCycleEnabled = partyCycleEnabled;
    }

    @Override
    public boolean isAutoAcceptInvites() {
        return autoAcceptInvites;
    }

    public void setAutoAcceptInvites(boolean autoAcceptInvites) {
        this.autoAcceptInvites = autoAcceptInvites;
    }

    @Override
    public List<String> getPartyInviteWhitelist() {
        return List.copyOf(partyInviteWhitelist);
    }

    public void setPartyInviteWhitelist(List<String> partyInviteWhitelist) {
        this.partyInviteWhitelist.clear();
        if (partyInviteWhitelist != null) {
            this.partyInviteWhitelist.addAll(partyInviteWhitelist);
        }
    }

    @Override
    public boolean isAutoCreateMatch() {
        return autoCreateMatch;
    }

    public void setAutoCreateMatch(boolean autoCreateMatch) {
        this.autoCreateMatch = autoCreateMatch;
    }

    @Override
    public boolean isAutoInviteFromMatching() {
        return autoInviteFromMatching;
    }

    public void setAutoInviteFromMatching(boolean autoInviteFromMatching) {
        this.autoInviteFromMatching = autoInviteFromMatching;
    }

    @Override
    public String getMatchingTitle() {
        return matchingTitle;
    }

    public void setMatchingTitle(String matchingTitle) {
        this.matchingTitle = matchingTitle != null ? matchingTitle : "";
    }

    @Override
    public boolean isHealOthers() {
        return healOthers;
    }

    public void setHealOthers(boolean healOthers) {
        this.healOthers = healOthers;
    }

    @Override
    public int getHealHpThresholdPercent() {
        return healHpThresholdPercent;
    }

    public void setHealHpThresholdPercent(int healHpThresholdPercent) {
        this.healHpThresholdPercent = healHpThresholdPercent;
    }

    @Override
    public int getHealSkillRefId() {
        return healSkillRefId;
    }

    public void setHealSkillRefId(int healSkillRefId) {
        this.healSkillRefId = healSkillRefId;
    }

    @Override
    public boolean isFollowLeader() {
        return followLeader;
    }

    public void setFollowLeader(boolean followLeader) {
        this.followLeader = followLeader;
    }

    @Override
    public float getFollowDistanceWorld() {
        return followDistanceWorld;
    }

    public void setFollowDistanceWorld(float followDistanceWorld) {
        this.followDistanceWorld = followDistanceWorld;
    }

    @Override
    public List<Integer> getBuffShareSkillIds() {
        return List.copyOf(buffShareSkillIds);
    }

    public void setBuffShareSkillIds(List<Integer> buffShareSkillIds) {
        this.buffShareSkillIds.clear();
        if (buffShareSkillIds != null) {
            this.buffShareSkillIds.addAll(buffShareSkillIds);
        }
    }

    @Override
    public IPartyPolicy toPolicy() {
        return PartyPolicy.builder()
                .partyCycleEnabled(partyCycleEnabled)
                .autoAcceptInvites(autoAcceptInvites)
                .partyInviteWhitelist(partyInviteWhitelist)
                .autoCreateMatch(autoCreateMatch)
                .autoInviteFromMatching(autoInviteFromMatching)
                .matchingTitle(matchingTitle)
                .healOthers(healOthers)
                .healHpThresholdPercent(healHpThresholdPercent)
                .healSkillRefId(healSkillRefId)
                .followLeader(followLeader)
                .followDistanceWorld(followDistanceWorld)
                .buffShareSkillIds(buffShareSkillIds)
                .build();
    }
}
