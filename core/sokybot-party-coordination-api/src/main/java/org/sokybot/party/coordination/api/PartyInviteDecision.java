package org.sokybot.party.coordination.api;

import java.util.Objects;

/**
 * Result of evaluating an invite (UI or coordinator policy hook).
 */
public final class PartyInviteDecision {

    private final boolean accept;
    private final String reason;

    public PartyInviteDecision(boolean accept) {
        this(accept, null);
    }

    public PartyInviteDecision(boolean accept, String reason) {
        this.accept = accept;
        this.reason = reason;
    }

    public boolean isAccept() {
        return accept;
    }

    /** Optional human-oriented reason for logging or UI; may be {@code null}. */
    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PartyInviteDecision)) {
            return false;
        }
        PartyInviteDecision that = (PartyInviteDecision) o;
        return accept == that.accept && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Boolean.valueOf(accept), reason);
    }
}
