package org.sokybot.gameevents.events.party;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sokybot.gameevents.events.core.IGameEvent;

import java.util.List;

@Getter
@Builder
@ToString
public class PartyMatchingListEvent implements IGameEvent {
    private final String fullName;
    private final long timestamp;

    private final byte result;
    private final byte pageCount;
    private final byte pageIndex;
    private final List<PartyMatchEntry> entries;

    @Getter
    @Builder
    @ToString
    public static class PartyMatchEntry {
        private final int partyNumber;
        private final int masterJid;
        private final String masterName;
        private final byte countryType;
        private final byte memberCount;
        private final byte settingsFlag;
        private final byte purposeType;
        private final byte levelMin;
        private final byte levelMax;
        private final String title;
    }
}
