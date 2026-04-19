package org.sokybot.party.coordination.api;

import java.util.Objects;

/**
 * Advertised party matching entry coordinated across machines.
 */
public final class PartyMatchPosting {

    private final String leaderMachine;
    private final String title;
    private final int minLevel;
    private final int maxLevel;

    public PartyMatchPosting(String leaderMachine, String title, int minLevel, int maxLevel) {
        this.leaderMachine = leaderMachine != null ? leaderMachine : "";
        this.title = title != null ? title : "";
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    public String getLeaderMachine() {
        return leaderMachine;
    }

    public String getTitle() {
        return title;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PartyMatchPosting)) {
            return false;
        }
        PartyMatchPosting that = (PartyMatchPosting) o;
        return minLevel == that.minLevel && maxLevel == that.maxLevel
                && leaderMachine.equals(that.leaderMachine) && title.equals(that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leaderMachine, title, minLevel, maxLevel);
    }
}
