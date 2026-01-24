package org.sokybot.gameevents.events.academy;

import lombok.Builder;
import lombok.Getter;
import org.sokybot.gameevents.events.core.IGameEvent;

import java.util.List;

/**
 * Event fired when academy matching list is received (opcode 0xB47D).
 */
@Getter
@Builder
public class AcademyMatchingListEvent implements IGameEvent {
    private final String fullName;
    private final long timestamp = System.currentTimeMillis();

    private final byte pageCount;
    private final byte pageIndex;
    private final List<AcademyMatch> matches;

    @Getter
    @Builder
    public static class AcademyMatch {
        private final int number;
        private final byte countryType;
        private final String title;
        private final byte displayLevel;
        private final byte leaderLevel;
        private final int leaderId;
        private final String leaderName;
        private final int graduatedCount;
        private final byte honorRank;
    }

    @Override
    public String getGroupName() {
        return fullName.split("\\.")[0];
    }

    @Override
    public String getMachineName() {
        return fullName.split("\\.")[1];
    }
}
