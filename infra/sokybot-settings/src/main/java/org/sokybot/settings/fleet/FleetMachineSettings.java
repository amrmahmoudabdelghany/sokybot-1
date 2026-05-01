package org.sokybot.settings.fleet;

/**
 * Per-machine fleet binding: which profile id this bot should align with (Epic #16).
 * Persisted under settings scope {@code fleet}.
 */
public final class FleetMachineSettings {

    /** Persisted settings scope id; must match the actuator registration. */
    public static final String SCOPE_NAME = "fleet";

    private String profileId = "";

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId == null ? "" : profileId.trim();
    }
}
