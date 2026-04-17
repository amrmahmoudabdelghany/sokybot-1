package org.sokybot.engine.api.event;

import org.sokybot.engine.api.EngineEvent;

import java.util.Objects;

public final class PartyIntent extends EngineEvent {
    public enum IntentType {
        INVITE,
        ACCEPT,
        DECLINE,
        LEAVE,
        DISBAND
    }

    private final String groupName;
    private final IntentType intentType;
    private final String targetMachineId;

    public PartyIntent(String groupName, IntentType intentType, String targetMachineId) {
        super("PARTY_INTENT");
        String normalizedGroup = Objects.requireNonNull(groupName, "groupName").trim();
        if (normalizedGroup.isEmpty()) {
            throw new IllegalArgumentException("groupName cannot be empty");
        }
        this.groupName = normalizedGroup;
        this.intentType = Objects.requireNonNull(intentType, "intentType");
        this.targetMachineId = targetMachineId;
    }

    public String getGroupName() {
        return groupName;
    }

    public IntentType getIntentType() {
        return intentType;
    }

    public String getTargetMachineId() {
        return targetMachineId;
    }
}
