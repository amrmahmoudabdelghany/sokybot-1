package org.sokybot.party.api;

import java.util.Objects;

/**
 * Summary row from party matching list (projection-mapped from game events).
 */
public final class PartyMatchListing {

    private final int partyNumber;
    private final String masterName;
    private final String title;
    private final int levelMin;
    private final int levelMax;

    public PartyMatchListing(int partyNumber, String masterName, String title, int levelMin, int levelMax) {
        this.partyNumber = partyNumber;
        this.masterName = masterName != null ? masterName : "";
        this.title = title != null ? title : "";
        this.levelMin = levelMin;
        this.levelMax = levelMax;
    }

    public int getPartyNumber() {
        return partyNumber;
    }

    public String getMasterName() {
        return masterName;
    }

    public String getTitle() {
        return title;
    }

    public int getLevelMin() {
        return levelMin;
    }

    public int getLevelMax() {
        return levelMax;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PartyMatchListing)) {
            return false;
        }
        PartyMatchListing that = (PartyMatchListing) o;
        return partyNumber == that.partyNumber && levelMin == that.levelMin && levelMax == that.levelMax
                && masterName.equals(that.masterName) && title.equals(that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partyNumber, masterName, title, levelMin, levelMax);
    }
}
