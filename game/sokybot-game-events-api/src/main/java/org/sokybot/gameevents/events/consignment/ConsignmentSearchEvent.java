package org.sokybot.gameevents.events.consignment;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sokybot.gameevents.events.core.IGameEvent;

import java.util.List;

@Getter
@Builder
@ToString
public class ConsignmentSearchEvent implements IGameEvent {
    private final String fullName;
    private final long timestamp;

    private final byte result;
    private final byte entryCount;
    private final byte pageCount;
    private final List<ConsignmentSearchEntry> entries;
    private final Integer errorCode;

    @Getter
    @Builder
    @ToString
    public static class ConsignmentSearchEntry {
        private final int personalId;
        private final String sellerName;
        private final byte saleStatus;
        private final int refItemId;
        private final int sellCount;
        private final long price;
        private final int regDate;
    }
}
